import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.awt.*;

public class FPitchDetect extends Compute {

    private double[] spectrum, noise;
    private int key;
    private ArrayList<Integer> peaks;
    private boolean recording, calibrating;
    private long calibStartTime;
    private double calibIndex;
    private JButton recordBtn, calibrateBtn, saveBtn;

    public FPitchDetect(String p) {
        super(p);
        peaks = new ArrayList<>();
        spectrum = new double[AudioStream.SR / 8];
        noise = new double[spectrum.length];
    }

    public void initializeButtonPanel() {
        super.initializeButtonPanel();

        calibrateBtn = new JButton("calibrate");
        calibrateBtn.setBackground(Color.LIGHT_GRAY);
        calibrateBtn.addActionListener(this);
        super.addBtn(calibrateBtn);

        recordBtn = new JButton("record");
        recordBtn.setBackground(Color.LIGHT_GRAY);
        recordBtn.addActionListener(this);
        super.addBtn(recordBtn);

        saveBtn = new JButton("save & exit");
        saveBtn.setBackground(Color.LIGHT_GRAY);
        saveBtn.addActionListener(this);
        super.addBtn(saveBtn);
    }

    public int compute(byte[] sample) {
        calcSpectrum(sample);
        if (calibrating) {
            calcNoise();
            calibIndex++;
            if (System.nanoTime() - calibStartTime > 2e9) {
                calibrating = false;
            }
        }
        if (recording) {
            peaks = detectPeak(500);
            key = calcKey(super.getKeys());
            return key;
        }
        return NULL_KEY;
    }

    public void calibrate() {
        calibStartTime = System.nanoTime();
        calibrating = true;
        calibIndex = 0;
        noise = new double[spectrum.length];
    }

    private void calcSpectrum(byte[] sample) {
        Complex[] waveC = new Complex[sample.length / 2];
        for (int i = 0; i < waveC.length; i++) {
            double frame = (sample[i * 2 + 1] << 8) + (sample[i * 2] & 0xFF);
            frame *= 0.5 - 0.5 * Math.cos(2 * Math.PI * i / waveC.length);
            waveC[i] = new Complex(frame, 0);
        }
        Complex[] spectrumC = Fourier.czt(waveC, AudioStream.F, AudioStream.SR);
        spectrum = new double[spectrumC.length];
        for (int i = 0; i < spectrum.length; i++) {
            spectrum[i] = spectrumC[i].abs();
            if (!calibrating) {
                spectrum[i] -= noise[i];
            }
        }

    }

    private void calcNoise() {
        for (int j = 0; j < spectrum.length; j++) {
            noise[j] *= calibIndex / (calibIndex + 1);
            noise[j] += spectrum[j] / (calibIndex + 1);
        }
    }

    private ArrayList<Integer> detectPeak(int width) {
        ArrayList<Integer> peaks = new ArrayList<>();
        for (int i = 0; i < spectrum.length; i++) {
            int j = 0;
            while (true) {
                double a = spectrum[i], b = spectrum[i];
                if (i - j >= 0) {
                    a = spectrum[i - j];
                }
                if (i + j < spectrum.length) {
                    b = spectrum[i + j];
                }
                if (i - j < 0 && i + j > spectrum.length) {
                    break;
                }
                if (a > spectrum[i] || b > spectrum[i]) {
                    break;
                }
                j++;
            }
            if (j >= width && spectrum[i] >= 800000 && i > 200) {
                peaks.add(i);
            }
        }
        return peaks;
    }

    private int calcKey(double[] keys) {
        double pitch;
        try {
            pitch = peaks.get(0) / 6.0;
        } catch (IndexOutOfBoundsException e) {
            return BLANK_KEY;
        }
        int lo = 0;
        int hi = keys.length - 1;
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            assert (mid < hi);
            double d1 = Math.abs(keys[mid] - pitch);
            double d2 = Math.abs(keys[mid + 1] - pitch);
            if (d2 <= d1) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return hi;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand() == "calibrate") {
            if (recording) {
                JOptionPane.showMessageDialog(this, "Recording.");
            } else {
                calibrate();
            }
        }
        if (e.getActionCommand() == "record") {
            if (calibrating) {
                JOptionPane.showMessageDialog(this, "Calibrating.");
            } else {
                recording = true;
                ((JButton)e.getSource()).setText("pause");
            }
        }
        if (e.getActionCommand() == "pause") {
            recording = false;
            ((JButton)e.getSource()).setText("record");
        }
        if (e.getActionCommand() == "save & exit") {
            super.save();
            System.exit(0);
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (calibrating) {
            g.setColor(Color.WHITE);
            int[] x = new int[1000];
            int[] y = new int[1000];
            for (int i = 0; i < 1000; i++) {
                x[i] = i;
                y[i] = 500 - (int)(noise[i * 6] / 20000);
            }
            g.drawPolyline(x, y, x.length);
        }
        if (recording) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 32));
            if (key != BLANK_KEY && key != NULL_KEY) {
                g.drawString(super.getKeyNames()[key], 500, 200);
            }
            for (int i = 0; i < 1000; i++) {
                g.fillRect(i, 500, 1, -(int)(spectrum[i * 6] / 20000));
            }
            g.setColor(Color.RED);
            for (Integer i : (ArrayList<Integer>)peaks.clone()) {
                g.fillOval(i / 6 - 4, 500 - (int)(spectrum[i] / 20000) - 4, 8, 8);
            }
        }
        repaint();
    }

}
