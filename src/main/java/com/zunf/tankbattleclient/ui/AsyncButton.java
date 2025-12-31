// AsyncButton.java
package com.zunf.tankbattleclient.ui;

import com.zunf.tankbattleclient.exception.BusinessException;
import com.zunf.tankbattleclient.exception.ErrorCode;
import com.zunf.tankbattleclient.model.bo.ResponseBo;
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

    private final ObjectProperty<Supplier<CompletableFuture<ResponseBo>>> action = new SimpleObjectProperty<>();

    private final ObjectProperty<Consumer<ResponseBo>> onSuccess = new SimpleObjectProperty<>();
    private final ObjectProperty<Consumer<BusinessException>> onError = new SimpleObjectProperty<>();

    private String normalText;
    private boolean permanentlyDisabled = false;

    public AsyncButton() {
        // 运行中自动禁用
        disableProperty().bind(running.getReadOnlyProperty());

        setOnAction(e -> runOnce());
    }

    public void runOnce() {
        if (running.get()) return;

        Supplier<CompletableFuture<ResponseBo>> a = action.get();
        Objects.requireNonNull(a, "AsyncButton.action must be set");

        running.set(true);
        normalText = getText();

        // 切换文案
        String lt = getLoadingText();
        String newText = (lt != null && !lt.isBlank()) ? lt : (normalText == null ? "" : normalText);
        if (isShowEllipsis()) newText += "...";
        setText(newText);

        final CompletableFuture<ResponseBo> f;
        try {
            f = a.get();
        } catch (Throwable t) {
            finishExceptionally(t);
            return;
        }
        if (f == null) {
            finishExceptionally(new BusinessException(ErrorCode.UNKNOWN_ERROR));
            return;
        }

        // 无论 future 在哪个线程完成，这里都保证回到 FX 线程恢复按钮 + 回调
        f.whenComplete((val, err) -> Platform.runLater(() -> {
            try {
                if (err == null) {
                    if (val != null) {
                        Consumer<ResponseBo> ok = onSuccess.get();
                        if (ok != null) ok.accept(val);
                    }
                } else {
                    Consumer<BusinessException> bad = onError.get();
                    if (bad != null) bad.accept(wrap(err));
                }
            } finally {
                // 如果按钮已被永久禁用，不再恢复状态
                if (!permanentlyDisabled) {
                    running.set(false);
                    setText(normalText);
                }
            }
        }));
    }

    private void finishExceptionally(Throwable t) {
        Platform.runLater(() -> {
            try {
                Consumer<BusinessException> bad = onError.get();
                if (bad != null) bad.accept(wrap(t));
            } finally {
                // 如果按钮已被永久禁用，不再恢复状态
                if (!permanentlyDisabled) {
                    running.set(false);
                    setText(normalText);
                }
            }
        });
    }

    private static BusinessException wrap(Throwable err) {
        if (err instanceof CompletionException) {
            err = err.getCause();
        }
        if (err instanceof BusinessException) {
            return (BusinessException) err;
        }
        return new BusinessException(ErrorCode.UNKNOWN_ERROR);
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

    public Supplier<CompletableFuture<ResponseBo>> getAction() { return action.get(); }
    public void setAction(Supplier<CompletableFuture<ResponseBo>> a) { action.set(a); }
    public ObjectProperty<Supplier<CompletableFuture<ResponseBo>>> actionProperty() { return action; }

    public Consumer<ResponseBo> getOnSuccess() { return onSuccess.get(); }
    public void setOnSuccess(Consumer<ResponseBo> c) { onSuccess.set(c); }
    public ObjectProperty<Consumer<ResponseBo>> onSuccessProperty() { return onSuccess; }

    public Consumer<BusinessException> getOnError() { return onError.get(); }
    public void setOnError(Consumer<BusinessException> c) { onError.set(c); }
    public ObjectProperty<Consumer<BusinessException>> onErrorProperty() { return onError; }

    /**
     * 永久禁用按钮并设置文本
     * 用于表示按钮已完成某个状态，不再需要交互
     * @param text 要显示的文本
     */
    public void setPermanentlyDisabled(String text) {
        Platform.runLater(() -> {
            permanentlyDisabled = true;
            // 解绑 disable 属性
            disableProperty().unbind();
            // 设置文本
            setText(text);
            // 禁用按钮
            setDisable(true);
            // 移除点击事件，防止再次点击
            setOnAction(null);
        });
    }

    /**
     * 检查按钮是否已被永久禁用
     */
    public boolean isPermanentlyDisabled() {
        return permanentlyDisabled;
    }
}
