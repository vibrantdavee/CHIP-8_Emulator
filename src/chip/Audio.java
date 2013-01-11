package chip;

import java.io.*;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/** Plays an audio file. */
public class Audio {

    public static void playSound(String file) {
        try{
            Clip clip = AudioSystem.getClip();
            AudioInputStream audioInput = AudioSystem.getAudioInputStream(new File(file));
            clip.open(audioInput);
            clip.start();
            System.out.println("Beep");
        } catch (Exception e) {
                            System.err.println("Failed to play audio file: " + e.getMessage());
        }
    }
}
