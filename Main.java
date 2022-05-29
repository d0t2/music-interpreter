import javax.swing.*;
import java.awt.*;
import java.io.File;

public class Main extends JFrame
{
    private AudioStream audio;
    private Compute compute;

    public Main(String p, int mode)
    {
        if(mode == Compute.FOURIER)
            compute = new FPitchDetect(p);
        if(mode == Compute.AUTOCORRELATION)
            compute = new ACPitchDetect(p);
        new Thread(compute).start();
        add(compute, BorderLayout.CENTER);
        add(compute.getButtonPanel(), BorderLayout.NORTH);

        audio = new AudioStream(compute);
        new Thread(audio).start();

        pack();
        setBackground(Color.DARK_GRAY);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    public static void main(String args[])
    {
        JButton fileBtn = new JButton();
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File("~"));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        String path = null;
        if(fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
            path = fc.getSelectedFile().getAbsolutePath();
        else
            System.exit(0);

        Main main = new Main(path, Compute.AUTOCORRELATION);
    }
}
