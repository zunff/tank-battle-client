package com.zunf.tankbattleclient.manager;

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

    public void playSoundEffect(String soundFileName) {
        try {
            AudioClip clip = soundEffects.get(soundFileName);
            if (clip == null) {
                URL resourceUrl = getClass().getResource(SOUND_RESOURCE_PATH + soundFileName);
                if (resourceUrl == null) {
                    System.err.println("Sound effect not found: " + soundFileName);
                    return;
                }
                clip = new AudioClip(resourceUrl.toExternalForm());
                soundEffects.put(soundFileName, clip);
            }
            clip.play();
        } catch (Exception e) {
            System.err.println("Failed to play sound effect: " + soundFileName + ", error: " + e.getMessage());
        }
    }

    public void playBackgroundMusic(String musicFileName, double volume) {
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

    public void cleanup() {
        stopBackgroundMusic();
        for (AudioClip clip : soundEffects.values()) {
            clip.stop();
        }
        soundEffects.clear();
        backgroundMusicCache.clear();
    }
}
