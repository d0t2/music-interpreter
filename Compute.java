
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.*;
import java.io.*;

public class Compute extends JPanel implements Runnable, ActionListener {
    public static final int NULL_KEY = -2;
    public static final int BLANK_KEY = -1;
    public static final int FOURIER = 0;
    public static final int AUTOCORRELATION = 1;
    private JPanel buttonPanel;
    private Queue<byte[]> sampleQ;
    private double[] keys;
    private String[] keyNames;
    private ArrayList<int[]> notes;
    private boolean running;
    private String path;
    private File dataFile;
    private FileOutputStream dataOut;

    public JPanel getButtonPanel() {
        return buttonPanel;
    }

    public double[] getKeys() {
        return keys;
    }

    public String[] getKeyNames() {
        return keyNames;
    }

    public void addBtn(JButton b) {
        buttonPanel.add(b);
    }

    public void addSample(byte[] s) {
        sampleQ.add(s);
    }

    public Compute(String p) {
        path = p;
        dataFile = new File(path + File.separator + "data.txt");
        dataFile.deleteOnExit();
        try {
            dataOut = new FileOutputStream(dataFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        sampleQ = new LinkedList<>();

        initializeButtonPanel();
        initializeKeyList();

        setBackground(Color.DARK_GRAY);
        setPreferredSize(new Dimension(1000, 500));
    }

    public void run() {
        try {
            running = true;
            while (running) {
                if (sampleQ.peek() == null) {
                    System.out.print("");
                    continue;
                }
                int key = compute(sampleQ.poll());
                if (key != NULL_KEY) {
                    dataOut.write(key);
                }
            }
            dataOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int compute(byte[] sample) {
        return NULL_KEY;
    }

    public void initializeKeyList() {
        keys = new double[88];
        keyNames = new String[88];

        double step = Math.pow(2, 1.0 / 12);
        String[] octave = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        for (int i = 0; i < keys.length; i++) {
            keys[i] = Math.pow(step, i - 48) * 440;
            keyNames[i] = octave[(i + 9) % 12] + "" + (i + 9) / 12;
        }
    }

    public void initializeButtonPanel() {
        buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.GRAY);
        buttonPanel.setLayout(new FlowLayout());
    }

    public void save() {
        running = false;
        notes = createNotes();
        createImage();
        createSound();
    }

    public ArrayList<int[]> createNotes() {
        try {
            ArrayList<int[]> notes = new ArrayList<>();
            FileInputStream reader = new FileInputStream(dataFile);

            int key = reader.read();
            int[] note = {key, 1};
            while ((key = reader.read()) != -1) {
                if (key != note[0]) {
                    notes.add(note.clone());
                    note[0] = key;
                    note[1] = 1;
                } else {
                    note[1]++;
                }
            }
            reader.close();

            return notes;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void createImage() {
        try {
            int width = 45, height = 1320;
            for (int[] n : notes)
                width += 10 * n[1];

            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = bi.createGraphics();

            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, width, height);

            g.setColor(Color.WHITE);
            int time = 0;
            for (int[] n : notes) {
                if (n[0] != 255) {
                    g.fillRect(time * 10, 1320 - n[0] * 15 - 15, n[1] * 10, 15);
                }
                time += n[1];
            }

            for (int i = 0; i < keyNames.length; i++) {
                if (keyNames[i].contains("#")) {
                    g.setColor(Color.DARK_GRAY);
                    g.fillRect(0, 1320 - i * 15 - 15, 45, 15);
                    g.setColor(Color.WHITE);
                    g.drawString(keyNames[i], 2, 1320 - i * 15 - 2);
                } else {
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 1320 - i * 15 - 15, 45, 15);
                    g.setColor(Color.DARK_GRAY);
                    g.drawString(keyNames[i], 2, 1320 - i * 15 - 2);
                }
                g.setColor(Color.DARK_GRAY);
                g.drawRect(0, 1320 - i * 15 - 15, 45, 15);
            }

            int index = 1;
            File imageFile = new File(path + File.separator + "notes" + index + ".png");
            while (imageFile.exists()) {
                index++;
                imageFile = new File(path + File.separator + "notes" + index + ".png");
            }

            ImageIO.write(bi, "PNG", imageFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createSound() {
        int index = 1;
        File soundFile = new File(path + File.separator + "melody" + index + ".wav");
        while (soundFile.exists()) {
            index++;
            soundFile = new File(path + File.separator + "melody" + index + ".wav");
        }

        try {
            double duration = 0;
            for (int[] n : notes)
                duration += n[1] / 8.0;

            WavFile wavFile = WavFile.newWavFile(soundFile, 2, (int)duration * AudioStream.SR, 16, AudioStream.SR);

            for (int[] n : notes) {
                int length = (int)(n[1] * AudioStream.SR / 8.0);
                double[][] buffer = new double[2][length];
                if (n[0] == 255) {
                    wavFile.writeFrames(buffer, buffer[0].length);
                    continue;
                }
                double pitch = keys[n[0]];
                for (int i = 0; i < buffer[0].length; i++) {
                    if ((int)(i * pitch * 8 / AudioStream.SR % 2) == 0) {
                        buffer[0][i] = 0.05;
                        buffer[1][i] = 0.05;
                    } else {
                        buffer[0][i] = -0.05;
                        buffer[1][i] = -0.05;
                    }
                }
                wavFile.writeFrames(buffer, buffer[0].length);
            }
            wavFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void actionPerformed(ActionEvent e) {}
}

