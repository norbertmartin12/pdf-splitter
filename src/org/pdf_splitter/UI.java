package org.pdf_splitter;

import java.io.File;
import java.util.Scanner;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;

public class UI {

    private static final String WINDOWS_LOOK_AND_FEEL = "windows";
    private final String title;
    private JFrame jFrame;

    public UI(String title) {
        this.title = title;
    }

    public void init() {
        for (LookAndFeelInfo lookAndFeel : UIManager.getInstalledLookAndFeels()) {
            if (lookAndFeel.getName().equalsIgnoreCase(WINDOWS_LOOK_AND_FEEL)) {
                try {
                    UIManager.setLookAndFeel(lookAndFeel.getClassName());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        println("#" + title);
        jFrame = new JFrame();
        jFrame.setTitle(title);
        jFrame.setSize(80, 20);
    }

    /**
     * @param title
     * @param openAt
     * @param fileFilter
     * @return null if none selected
     */
    public File requestFile(String title, File openAt, FileFilter fileFilter) {
        checkInit();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(this.title + " - " + title);
        chooser.setCurrentDirectory(openAt);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(fileFilter);
        int option = chooser.showOpenDialog(jFrame);
        if (option == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    /**
     * @param title
     * @param openAt
     * @return null if none selected
     */
    public File requestOutputFolder(String title, File openAt) {
        checkInit();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(this.title + " - " + title);
        chooser.setCurrentDirectory(openAt);
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int option = chooser.showOpenDialog(jFrame);
        if (option == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    public String requestPassword(Scanner scanner) {
        print("> saisir le mot de passe: ");
        return scanner.nextLine();
    }

    public int requestNbPagesPerChildDocument(Scanner scanner, int nbPages) {
        int inputNbPagePerDocument = -1;
        while (inputNbPagePerDocument <= 0) {
            print("> nombre de pages par sous document ? ");
            inputNbPagePerDocument = scanner.nextInt();
        }
        return inputNbPagePerDocument;
    }

    private void checkInit() {
        if (jFrame == null) {
            init();
        }
    }

    public UI println(String msg) {
        System.out.println(msg);
        return this;
    }

    public UI print(String msg) {
        System.out.print(msg);
        return this;
    }

    public UI errPrintln(String msg) {
        System.err.println(msg);
        return this;
    }

    /**
     * @param description
     * @param extension
     * @return
     */
    public FileFilter fileFilter(final String description, final String extension) {
        return new FileFilter() {
            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(extension) || file.isDirectory();
            }
        };
    }

    public void release() {
        if (jFrame != null) {
            jFrame.dispose();
        }
    }
}
