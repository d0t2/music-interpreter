import javax.sound.sampled.*;

public class AudioStream implements Runnable {
    public static final int F = 1000;
    public static final int SR = 48000;
    private Compute compute;
    private byte[] sample;
    private TargetDataLine line;
    private boolean running;

    public AudioStream(Compute c) {
        compute = c;
        sample = new byte[SR / 4];

        AudioFormat format = new AudioFormat(SR, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        try {
            line = (TargetDataLine)AudioSystem.getLine(info);
            line.open(format);
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        }
    }

    public void run() {
        line.start();

        running = true;
        while (running) {
            line.read(sample, 0, sample.length);
            compute.addSample(sample);
        }

        line.close();
    }

    public void stop() {
        running = false;
    }
}
