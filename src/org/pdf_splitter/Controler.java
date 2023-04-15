package org.pdf_splitter;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.pdfbox.pdmodel.PDDocument;

public class Controler {

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
        Scanner scanner = null;
        PDDocument inPDF = null;
        List<PDDocument> outPdDocuments = null;
        try {
            scanner = new Scanner(System.in);

            File pdfFile = ui.requestFile("Document à traiter", EXEC_FOLDER, ui.fileFilter("Fichier PDF", Tool.PDF_EXTENSION));
            if (pdfFile == null) {
                exitWithMessage("#aucun document sélectionné, fin du programme");
            }

            inPDF = tool.openPdf(pdfFile, scanner);

            Map<Integer, AgentBookmark> bookmarksByPage = tool.identifyBookmarks(inPDF);
            if (bookmarksByPage.isEmpty()) {
                exitWithMessage("#aucun bookmark trouvé dans le PDF, fin du programme");
            }

            outPdDocuments = tool.splitIntoChildFiles(inPDF, bookmarksByPage);
            if (outPdDocuments == null) {
                exitOnException("generate files", new ProgramExceptionExit("no child pdf generated"));
            }

            File outputFolder = ui.requestOutputFolder("Dossier destination", pdfFile.getParentFile());
            if (outputFolder == null) {
                exitWithMessage("#aucun dossier sélectionné, fin du programme");
            }

            File toSendFolder = new File(outputFolder + File.separator + Tool.TO_SEND_FOLDER);
            tool.generateGroupedOutputChildPdfs(toSendFolder, outPdDocuments);

            File unknownFolder = new File(outputFolder + File.separator + Tool.INCONNUS);
            String periodSubfolder = new SimpleDateFormat("yyyy " + File.separator + "MM").format(new Date());
            tool.generateDispatchedOutputChildPdfs(outPdDocuments, outputFolder, periodSubfolder, unknownFolder);

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
                    exitOnException("pdf close", e);
                }
            }
            if (outPdDocuments != null) {
                for (PDDocument outPdDocument : outPdDocuments) {
                    try {
                        outPdDocument.close();
                    } catch (IOException e) {
                        exitOnException("out documents close", e);
                    }
                }
            }
        }
    }

    private void exitWithMessage(String msg) {
        ui.println(msg);
        System.exit(1);
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
