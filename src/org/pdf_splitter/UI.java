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
    private JFrame parentInvisbileJFrame;

    public UI(String title) {
        this.title = title;
    }

    /**
     * Initializes parent component for future dialog usages
     */
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
        parentInvisbileJFrame = new JFrame();
        parentInvisbileJFrame.setTitle(title);
        parentInvisbileJFrame.setSize(80, 20);
    }

    /**
     * Releases parent component
     */
    public void release() {
        if (parentInvisbileJFrame != null) {
            parentInvisbileJFrame.dispose();
        }
    }

    /**
     * Requests file
     * 
     * @param title
     * @param openAt
     * @param fileFilter
     * @return null if none selected
     */
    public File requestFile(String title, File openAt, FileFilter fileFilter) {
        println("Bonjour " + System.getProperty("user.name"));
        println("> sélectionner le document à découper");
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(this.title + " - " + title);
        chooser.setCurrentDirectory(openAt);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(fileFilter);
        int option = chooser.showOpenDialog(parentInvisbileJFrame);
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
        println("> Sélectionner le dossier destination");
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(this.title + " - " + title);
        chooser.setCurrentDirectory(openAt);
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int option = chooser.showOpenDialog(parentInvisbileJFrame);
        if (option == JFileChooser.APPROVE_OPTION) {
            println("#dossier sortie: " + chooser.getSelectedFile().getAbsolutePath());
            return chooser.getSelectedFile();
        }
        return null;
    }

    public String requestPassword(Scanner scanner) {
        print("> saisir le mot de passe: ");
        return scanner.nextLine();
    }

    /**
     * Prints out message and ends line
     * 
     * @param msg
     * @return this
     */
    public UI println(String msg) {
        System.out.println(msg);
        return this;
    }

    /**
     * Prints out message
     * 
     * @param msg
     * @return this
     */
    public UI print(String msg) {
        System.out.print(msg);
        return this;
    }

    /**
     * Prints err message and ends line
     * 
     * @param msg
     * @return this
     */
    public UI errPrintln(String msg) {
        System.err.println(msg);
        return this;
    }

    /**
     * @param description
     * @param extension
     * @return associated FileFilter
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

}
