import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

public class ACPitchDetect extends Compute {
    private double[] autoCorrelation;
    private double[] spectrum;
    private int key;
    private JButton recordBtn, saveBtn;
    private boolean recording;
    private ArrayList<Integer> volume;
    private ArrayList<Integer> peaks;

    public ACPitchDetect(String p) {
        super(p);
        volume = new ArrayList<>();
        peaks = new ArrayList<>();
        autoCorrelation = new double[AudioStream.SR / 16];
        spectrum = new double[autoCorrelation.length];
    }

    public void initializeButtonPanel() {
        super.initializeButtonPanel();

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
        int[] wave = wave(sample);
        if (!recording) {
            return NULL_KEY;
        }

        int max = wave[0];
        for (int i = 0; i < wave.length / 2; i++) {
            max = Math.max(wave[i], max);
        }
        volume.add(max);
        for (int i = wave.length / 2; i < wave.length; i++) {
            max = Math.max(wave[i], max);
        }
        volume.add(max);
        if (max < 3000) {
            spectrum = new double[autoCorrelation.length];
            autoCorrelation = new double[autoCorrelation.length];
            return BLANK_KEY;
        }

        autoCorrelation = autoCorrelate(wave);

        key = calcKey(super.getKeys());
        return key;
    }

    private ArrayList<Integer> detectPeak(int width) {
        peaks = new ArrayList<>();
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
            if (j >= width && spectrum[i] > 100) {
                peaks.add(i);
            }
        }

        return peaks;
    }

    private int calcKey(double[] keys) {
        Complex[] autoCorrelationC = new Complex[autoCorrelation.length];
        for (int i = 0; i < autoCorrelationC.length; i++) {
            autoCorrelationC[i] = new Complex(autoCorrelation[i], 0);
        }

        Complex[] spectrumC = Fourier.czt(autoCorrelationC, AudioStream.F, AudioStream.SR);
        spectrum = new double[autoCorrelation.length];
        for (int i = 0; i < spectrumC.length; i++) {
            spectrum[i] = spectrumC[i].abs();
        }

        ArrayList<Integer> peaks = detectPeak(50);
        double pitch;
        try {
            pitch = peaks.get(0) / 3.0;
        } catch (Exception e) {
            return BLANK_KEY;
        }

        if (pitch > keys[64] || pitch < keys[16]) {
            return BLANK_KEY;
        }

        int lo = 0;
        int hi = keys.length - 1;
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            assert (mid < hi);
            double d1 = Math.abs(keys[mid] - pitch);
            double d2 = Math.abs(keys[mid + 1] - pitch);
            if (d2 <= d1)
                lo = mid + 1;
            else
                hi = mid;
        }
        return hi;
    }

    private int[] wave(byte[] sample) {
        int[] wave = new int[sample.length / 2];
        for (int i = 0; i < wave.length; i++) {
            wave[i] = (sample[i * 2 + 1] << 8) + (sample[i * 2] & 0xFF);
        }

        return wave;
    }

    private double[] autoCorrelate(int[] ar) {
        int[] ar1 = new int[ar.length / 2];
        int[] ar2 = new int[ar.length / 2];
        double[] output = new double[ar.length / 2];

        for (int i = 0; i < ar1.length; i++) {
            ar1[i] = ar[i];
        }

        int offset = 0;
        while (offset < ar2.length) {
            for (int i = 0; i < ar2.length; i++) {
                ar2[i] = ar[i + offset];
            }

            output[offset] = correlation(ar1, ar2);
            offset++;
        }

        for (int i = 0; i < output.length; i++)
            output[i] *= 0.543478 - 0.543478 * Math.cos(2 * Math.PI * i / output.length);

        return output;
    }

    private double correlation(int[] x, int[] y) {
        int[] xy = new int[x.length];
        int[] xSqr = new int[x.length];
        int[] ySqr = new int[x.length];
        for (int i = 0; i < x.length; i++) {
            xy[i] = x[i] * y[i];
            xSqr[i] = x[i] * x[i];
            ySqr[i] = y[i] * y[i];
        }

        double output = avg(xy) - avg(x) * avg(y);
        output /= Math.sqrt(avg(xSqr) - avg(x) * avg(x));
        output /= Math.sqrt(avg(ySqr) - avg(y) * avg(y));
        return output;
    }

    private double avg(int[] ar) {
        long sum = 0;
        for (int i : ar)
            sum += i;

        return (double)sum / ar.length;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand() == "record") {
            recording = true;
            ((JButton)e.getSource()).setText("pause");
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

        if (!recording) {
            repaint();
            return;
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 32));

        if (key != BLANK_KEY && key != NULL_KEY) {
            g.drawString(super.getKeyNames()[key], 500, 200);
        }

        for (int i = 0; i < 1000; i++) {
            g.fillRect(i, -(int)(autoCorrelation[i * 3] * 100) + 200, 1, 1);
            g.fillRect(i, -(int)(spectrum[i * 3] / 10) + 500, 1, 1);
        }

        for (int i = 0; i < volume.size(); i++) {
            g.fillRect(i, 0, 1, volume.get(i) / 100);
        }

        g.setColor(Color.RED);
        for (int i = 0; i < peaks.size(); i++) {
            g.fillOval((peaks.get(i) / 3) - 2, -(int)(spectrum[peaks.get(i)] / 10) + 500 - 2, 4, 4);
        }

        repaint();
    }
}
