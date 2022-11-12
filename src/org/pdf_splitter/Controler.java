package org.pdf_splitter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.pdfbox.pdmodel.PDDocument;

public class Controler {

    private static final String INCONNUS = "inconnus";
    private static final String BULLETINS = "bulletins";
    // TODO : simplify meta data read/write
    // TODO : question ? create or not all path if doesn't exist - new year ?
    private static final File EXEC_FOLDER = new File(".");
    private final UI ui;
    private final Tool tool;

    public Controler(UI ui) {
        if (ui == null) {
            throw new IllegalArgumentException("ui= " + ui);
        }
        this.ui = ui;
        this.tool = new Tool(ui);
    }

    public void launch() {
        ui.init();
        Scanner scanner = new Scanner(System.in);
        PDDocument inPDF = null;
        List<PDDocument> outPdDocuments = null;
        try {
            ui.println("Bonjour " + System.getProperty("user.name"));
            ui.println("> sélectionner le document à découper");
            File pdfFile = ui.requestFile("Document à traiter", EXEC_FOLDER, ui.fileFilter("Fichier PDF", Tool.PDF_EXTENSION));
            if (pdfFile == null) {
                ui.println("#aucun document sélectionné, fin du programme");
                return;
            }
            inPDF = tool.openPdf(pdfFile, scanner);
            Map<Integer, AgentBookmark> bookmarksByPage = tool.identifyBookmarks(inPDF);
            if (bookmarksByPage.size() > 0) {
                ui.println("#" + bookmarksByPage.size() + " signets trouvés");
                outPdDocuments = tool.generateFiles(inPDF, bookmarksByPage);
            } else {
                int nbPagesPerDocument = ui.requestNbPagesPerChildDocument(scanner, inPDF.getPages().getCount());
                if (inPDF.getPages().getCount() % nbPagesPerDocument != 0) {
                    ui.errPrintln("!" + inPDF.getPages().getCount() + " n'est pas un multiple de " + nbPagesPerDocument + " le dernier document aura moins de pages");
                }
                outPdDocuments = tool.generateRawOutputFiles(inPDF, nbPagesPerDocument);
            }
            if (outPdDocuments == null) {
                exitOnException("generate files", new IllegalStateException("no child pdf generated"));
            }
            ui.println("> Sélectionner le dossier destination");
            File outputFolder = ui.requestOutputFolder("Dossier destination", pdfFile.getParentFile());
            if (outputFolder == null) {
                ui.println("#aucun dossier sélectionné, fin du programme");
                return;
            }
            ui.println("#dossier sortie: " + outputFolder.getAbsolutePath());

            File toSendFolder = new File(outputFolder + File.separator + Tool.TO_SEND_FOLDER);
            ui.println("#génération groupée: " + toSendFolder.getAbsolutePath());
            toSendFolder.mkdir();
            for (PDDocument outPdDocument : outPdDocuments) {
                String fileName = outPdDocument.getDocumentCatalog().getMetadata().getCOSObject().getString(Tool.DATA_FILENAME);
                File outFile = new File(toSendFolder + File.separator + BULLETINS + File.separator + fileName);
                tool.saveFile(outFile, outPdDocument);
            }

            ui.println("#rangement des fichiers");
            File unknownFolder = new File(outputFolder + File.separator + INCONNUS);
            for (PDDocument outPdDocument : outPdDocuments) {
                String searchedFolder = outPdDocument.getDocumentCatalog().getMetadata().getCOSObject().getString(Tool.DATA_FOLDER);
                File targetFolder = null;
                if (!searchedFolder.equals(Tool.UNKNOWN_FOLDER)) {
                    targetFolder = tool.searchFolder(outputFolder, searchedFolder);
                    if (targetFolder == null) {
                        targetFolder = tool.magicSearchFolder(outputFolder, searchedFolder);
                    }
                }
                if (targetFolder == null) {
                    targetFolder = unknownFolder;
                    ui.errPrintln("!" + searchedFolder + " non trouvé");
                } else {
                    ui.println(searchedFolder + " trouvé " + targetFolder.getAbsolutePath());
                    targetFolder = new File(targetFolder.getAbsolutePath() + File.separator + BULLETINS);
                }
                try {
                    String fileName = outPdDocument.getDocumentCatalog().getMetadata().getCOSObject().getString(Tool.DATA_FILENAME);
                    tool.saveFile(new File(targetFolder + File.separator + fileName), outPdDocument);
                } catch (IOException e) {
                    exitOnException("save file", e);
                }
            }
            ui.println("").println("#les documents ont été générés et rangés");
            if (unknownFolder.exists() && unknownFolder.list().length > 0) {
                ui.errPrintln("#!" + unknownFolder.getAbsolutePath());
                ui.errPrintln("#!" + unknownFolder.listFiles().length + " documents non rangés (ce traitement ou un précdent)");
                ui.errPrintln("#!rangez manuellement ou supprimez les pour faire disparaitre cette alerte");
            }
        } catch (ProgramExceptionExit e) {
            exitOnException("launch - managed exception", e);
        } catch (Exception e) {
            exitOnException("launch - unmanaged exception", e);
        } finally {
            ui.release();
            if (scanner != null) {
                scanner.close();
            }
            if (inPDF != null) {
                try {
                    inPDF.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outPdDocuments != null) {
                for (PDDocument outPdDocument : outPdDocuments) {
                    try {
                        outPdDocument.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void exitOnException(String step, Exception e) {
        ui.errPrintln("#!désolé une erreur est survenue: " + step);
        ui.errPrintln("#!contactez le développeur, mailto:norbertmartin12@gmail.com");
        if (e != null) {
            e.printStackTrace();
        }
        System.exit(-1);
    }

    UI getUi() {
        return ui;
    }
}
