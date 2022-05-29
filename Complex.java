
public class Complex {
    public double real, imaginary;

    public Complex() {
        real = 0;
        imaginary = 0;
    }

    public Complex(double r, double i) {
        real = r;
        imaginary = i;
    }

    public String toString() {
        return real + " + " + imaginary + "i";
    }

    public Complex plus(Complex c) {
        double r = this.real + c.real;
        double i = this.imaginary + c.imaginary;
        return new Complex(r, i);
    }

    public Complex minus(Complex c) {
        double r = this.real - c.real;
        double i = this.imaginary - c.imaginary;
        return new Complex(r, i);
    }

    public Complex times(Complex c) {
        double r = this.real * c.real - this.imaginary * c.imaginary;
        double i = this.real * c.imaginary + this.imaginary * c.real;
        return new Complex(r, i);
    }

    public Complex scale(double s) {
        return new Complex(s * real, s * imaginary);
    }

    public Complex conjugate() {
        return new Complex(real, -imaginary);
    }

    public double abs() {
        return Math.hypot(real, imaginary);
    }
}
