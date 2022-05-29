
public class Fourier {
    public static Complex rootu(double k, int n) {
        double theta = 2 * Math.PI / n;
        return new Complex(Math.cos(theta * k), Math.sin(theta * k));
    }

    public static Complex[] fft(Complex[] waveform) {
        int n = waveform.length;

        if (n == 1) {
            return new Complex[]{waveform[0]};
        }

        Complex[] even = new Complex[n / 2];
        for (int k = 0; k < n / 2; k++) {
            even[k] = waveform[2 * k];
        }
        Complex[] even2 = fft(even);

        Complex[] odd = new Complex[n / 2];
        for (int k = 0; k < n / 2; k++) {
            odd[k] = waveform[2 * k + 1];
        }
        Complex[] odd2 = fft(odd);

        Complex[] output = new Complex[n];
        for (int k = 0; k < n / 2; k++) {
            Complex wk = rootu(-k, n);
            output[k] = even2[k].plus(wk.times(odd2[k]));
            output[k + n / 2] = even2[k].minus(wk.times(odd2[k]));
        }

        return output;
    }

    public static Complex[] ifft(Complex[] spectrum) {
        int n = spectrum.length;

        Complex[] output = new Complex[n];

        for (int i = 0; i < n; i++) {
            output[i] = spectrum[i].conjugate();
        }

        output = fft(output);

        for (int i = 0; i < n; i++) {
            output[i] = output[i].conjugate();
            output[i] = output[i].scale(1.0 / n);
        }

        return output;
    }

    public static Complex[] convolve(Complex[] x, Complex[] y) {
        int n = x.length;

        Complex[] a = fft(x);
        Complex[] b = fft(y);

        Complex[] c = new Complex[n];

        for (int i = 0; i < n; i++) {
            c[i] = a[i].times(b[i]);
        }

        return ifft(c);
    }

    public static Complex[] czt(Complex[] waveform, double f, double sr) {
        int n = waveform.length;
        int m = Integer.highestOneBit(n) * 4;

        Complex w;
        Complex[] wf = new Complex[m];
        Complex[] xwn = new Complex[m];
        Complex[] wn = new Complex[m];

        for (int i = 0; i < m; i++) {
            wf[i] = new Complex();
            xwn[i] = new Complex();
            wn[i] = new Complex();
        }

        for (int i = 0; i < n; i++) {
            w = rootu(.5 * i * i * (f / sr), n);

            wf[i].real = w.real;
            wf[i].imaginary = -w.imaginary;

            xwn[i] = waveform[i].times(wf[i]);
            wn[i] = w;
            wn[m - i - 1] = w;
        }

        Complex[] convolution = convolve(xwn, wn);
        Complex[] output = new Complex[n];
        for (int i = 0; i < n; i++) {
            output[i] = wf[i].times(convolution[i]);
        }

        return output;
    }
}
