// AsyncButton.java
package com.zunf.tankbattleclient.ui;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.control.Button;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 点击后自动进入“运行中”状态（禁用 + 文案变更），
 * 等 CompletableFuture 完成后自动恢复，并在 FX 线程回调 onSuccess/onError。
 *
 * 用法：setAction(() -> CompletableFuture<T>)
 */
public class AsyncButton extends Button {

    private final ReadOnlyBooleanWrapper running = new ReadOnlyBooleanWrapper(false);

    private final StringProperty loadingText = new SimpleStringProperty("");
    private final BooleanProperty showEllipsis = new SimpleBooleanProperty(true);

    private final ObjectProperty<Supplier<CompletableFuture<?>>> action = new SimpleObjectProperty<>();

    private final ObjectProperty<Consumer<Object>> onSuccess = new SimpleObjectProperty<>();
    private final ObjectProperty<Consumer<Throwable>> onError = new SimpleObjectProperty<>();

    private String normalText;

    public AsyncButton() {
        // 运行中自动禁用
        disableProperty().bind(running.getReadOnlyProperty());

        setOnAction(e -> runOnce());
    }

    public void runOnce() {
        if (running.get()) return;

        Supplier<CompletableFuture<?>> a = action.get();
        Objects.requireNonNull(a, "AsyncButton.action must be set");

        running.set(true);
        normalText = getText();

        // 切换文案
        String lt = getLoadingText();
        String newText = (lt != null && !lt.isBlank()) ? lt : (normalText == null ? "" : normalText);
        if (isShowEllipsis()) newText += "...";
        setText(newText);

        final CompletableFuture<?> f;
        try {
            f = a.get();
        } catch (Throwable t) {
            finishExceptionally(t);
            return;
        }
        if (f == null) {
            finishExceptionally(new NullPointerException("AsyncButton.action returned null future"));
            return;
        }

        // 无论 future 在哪个线程完成，这里都保证回到 FX 线程恢复按钮 + 回调
        f.whenComplete((val, err) -> Platform.runLater(() -> {
            try {
                if (err == null) {
                    Consumer<Object> ok = onSuccess.get();
                    if (ok != null) ok.accept(val);
                } else {
                    Consumer<Throwable> bad = onError.get();
                    if (bad != null) bad.accept(unwrap(err));
                }
            } finally {
                running.set(false);
                setText(normalText);
            }
        }));
    }

    private void finishExceptionally(Throwable t) {
        Platform.runLater(() -> {
            try {
                Consumer<Throwable> bad = onError.get();
                if (bad != null) bad.accept(t);
            } finally {
                running.set(false);
                setText(normalText);
            }
        });
    }

    private static Throwable unwrap(Throwable err) {
        if (err instanceof CompletionException ce && ce.getCause() != null) return ce.getCause();
        return err;
    }

    // -------- properties --------

    public boolean isRunning() { return running.get(); }
    public ReadOnlyBooleanProperty runningProperty() { return running.getReadOnlyProperty(); }

    public String getLoadingText() { return loadingText.get(); }
    public void setLoadingText(String v) { loadingText.set(v); }
    public StringProperty loadingTextProperty() { return loadingText; }

    public boolean isShowEllipsis() { return showEllipsis.get(); }
    public void setShowEllipsis(boolean v) { showEllipsis.set(v); }
    public BooleanProperty showEllipsisProperty() { return showEllipsis; }

    public Supplier<CompletableFuture<?>> getAction() { return action.get(); }
    public void setAction(Supplier<CompletableFuture<?>> a) { action.set(a); }
    public ObjectProperty<Supplier<CompletableFuture<?>>> actionProperty() { return action; }

    public Consumer<Object> getOnSuccess() { return onSuccess.get(); }
    public void setOnSuccess(Consumer<Object> c) { onSuccess.set(c); }
    public ObjectProperty<Consumer<Object>> onSuccessProperty() { return onSuccess; }

    public Consumer<Throwable> getOnError() { return onError.get(); }
    public void setOnError(Consumer<Throwable> c) { onError.set(c); }
    public ObjectProperty<Consumer<Throwable>> onErrorProperty() { return onError; }
}
