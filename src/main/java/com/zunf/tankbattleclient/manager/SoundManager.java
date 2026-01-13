package com.zunf.tankbattleclient.manager;

import javafx.application.Platform;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {

    private static SoundManager instance;

    private MediaPlayer currentBackgroundMusic;

    private final Map<String, AudioClip> soundEffects;
    private final Map<String, Media> backgroundMusicCache;
    
    // 声音开关状态
    private boolean soundEnabled = true;
    private boolean musicEnabled = true;

    private static final String SOUND_RESOURCE_PATH = "/source/";

    private SoundManager() {
        soundEffects = new HashMap<>();
        backgroundMusicCache = new HashMap<>();
    }

    public static synchronized SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    /**
     * 预加载音效，避免首次播放时的延迟
     * 在 JavaFX 应用线程中创建并预热音频系统
     */
    public void preloadSoundEffect(String soundFileName) {
        if (soundEffects.containsKey(soundFileName)) {
            return; // 已经加载过了
        }
        
        // 确保在 JavaFX 应用线程中执行
        Platform.runLater(() -> {
            try {
                URL resourceUrl = getClass().getResource(SOUND_RESOURCE_PATH + soundFileName);
                if (resourceUrl == null) {
                    System.err.println("Sound effect not found: " + soundFileName);
                    return;
                }
                AudioClip clip = new AudioClip(resourceUrl.toExternalForm());
                soundEffects.put(soundFileName, clip);
                
                // 预热：静音播放然后立即停止，确保音频系统准备好
                // 这样下次播放时就不会有延迟了
                clip.play(0.0); // 0.0 音量，用户听不到
                clip.stop();
            } catch (Exception e) {
                System.err.println("Failed to preload sound effect: " + soundFileName + ", error: " + e.getMessage());
            }
        });
    }

    /**
     * 播放音效（使用默认音量 1.0）
     */
    public void playSoundEffect(String soundFileName) {
        playSoundEffect(soundFileName, 1.0);
    }

    /**
     * 播放音效，支持音量控制
     * @param soundFileName 音效文件名
     * @param volume 音量 (0.0 - 1.0)
     */
    public void playSoundEffect(String soundFileName, double volume) {
        if (!soundEnabled) {
            return; // 音效已关闭
        }
        
        try {
            AudioClip clip = soundEffects.get(soundFileName);
            if (clip == null) {
                // 如果未预加载，则立即加载（可能会有延迟）
                URL resourceUrl = getClass().getResource(SOUND_RESOURCE_PATH + soundFileName);
                if (resourceUrl == null) {
                    System.err.println("Sound effect not found: " + soundFileName);
                    return;
                }
                clip = new AudioClip(resourceUrl.toExternalForm());
                soundEffects.put(soundFileName, clip);
            }
            clip.play(volume);
        } catch (Exception e) {
            System.err.println("Failed to play sound effect: " + soundFileName + ", error: " + e.getMessage());
        }
    }

    public void playBackgroundMusic(String musicFileName, double volume) {
        if (!musicEnabled) {
            return; // 背景音乐已关闭
        }
        
        try {
            Media media = backgroundMusicCache.get(musicFileName);
            if (media == null) {
                URL resourceUrl = getClass().getResource(SOUND_RESOURCE_PATH + musicFileName);
                if (resourceUrl == null) {
                    System.err.println("Background music not found: " + musicFileName);
                    return;
                }
                media = new Media(resourceUrl.toExternalForm());
                backgroundMusicCache.put(musicFileName, media);
            }

            if (currentBackgroundMusic != null) {
                if (currentBackgroundMusic.getMedia().getSource().contains(musicFileName)) {
                    return;
                }
                stopBackgroundMusic();
            }

            currentBackgroundMusic = new MediaPlayer(media);
            currentBackgroundMusic.setCycleCount(MediaPlayer.INDEFINITE);
            currentBackgroundMusic.setVolume(volume);
            currentBackgroundMusic.play();
        } catch (Exception e) {
            System.err.println("Failed to play background music: " + musicFileName + ", error: " + e.getMessage());
        }
    }

    public void stopBackgroundMusic() {
        if (currentBackgroundMusic != null) {
            currentBackgroundMusic.stop();
            currentBackgroundMusic.dispose();
            currentBackgroundMusic = null;
        }
    }

    public void setBackgroundMusicVolume(double volume) {
        if (currentBackgroundMusic != null) {
            currentBackgroundMusic.setVolume(volume);
        }
    }

    public void pauseBackgroundMusic() {
        if (currentBackgroundMusic != null) {
            currentBackgroundMusic.pause();
        }
    }

    public void resumeBackgroundMusic() {
        if (currentBackgroundMusic != null) {
            currentBackgroundMusic.play();
        }
    }

    /**
     * 设置音效开关
     */
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        if (!enabled) {
            // 关闭时停止所有正在播放的音效
            for (AudioClip clip : soundEffects.values()) {
                clip.stop();
            }
        }
    }

    /**
     * 设置背景音乐开关
     */
    public void setMusicEnabled(boolean enabled) {
        this.musicEnabled = enabled;
        if (!enabled) {
            pauseBackgroundMusic();
        } else {
            resumeBackgroundMusic();
        }
    }

    /**
     * 切换所有声音（音效+背景音乐）
     */
    public void toggleAllSounds(boolean enabled) {
        setSoundEnabled(enabled);
        setMusicEnabled(enabled);
    }

    /**
     * 获取音效开关状态
     */
    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    /**
     * 获取背景音乐开关状态
     */
    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    public void cleanup() {
        stopBackgroundMusic();
        for (AudioClip clip : soundEffects.values()) {
            clip.stop();
        }
        soundEffects.clear();
        backgroundMusicCache.clear();
    }
}
