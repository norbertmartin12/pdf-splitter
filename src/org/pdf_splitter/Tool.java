package org.pdf_splitter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

public class Tool {

    public static final String TO_SEND_FOLDER = "TO_SEND";
    public static final String DATA_FILENAME = "pdfsplitter_title";
    public static final String DATA_FOLDER = "pdfsplitter_folder";
    public static final String UNKNOWN_FOLDER = "pdfsplitter_unknown_folder";
    public static final String PDF_EXTENSION = ".pdf";
    public static final String INCONNUS = "inconnus";
    private static final String NO_PASSWORD = "";
    private static final String BULLETINS = "bulletins";

    private static final FileFilter FOLDER_FILTER = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }
    };

    private final UI ui;

    public Tool(UI ui) {
        this.ui = ui;
    }

    /**
     * Opens given pdf (manages password)
     * 
     * @param pdfFile
     * @param scanner
     * @return opened document
     * @throws ProgramExceptionExit
     */
    public PDDocument openPdf(File pdfFile, Scanner scanner) throws ProgramExceptionExit {
        // 1st try without password
        PDDocument inPDF = null;
        try {
            ui.println("#ouverture : " + pdfFile.getAbsolutePath());
            inPDF = PDDocument.load(pdfFile, NO_PASSWORD);
            return inPDF;
        } catch (InvalidPasswordException e) {
            // retry if password exception
            int retry = 1;
            while (inPDF == null && retry <= 2) {
                String password = ui.requestPassword(scanner);
                try {
                    inPDF = PDDocument.load(pdfFile, password);
                    return inPDF;
                } catch (InvalidPasswordException e1) {
                    ui.errPrintln("! le mot de passe  ne fonctionne pas");
                    retry++;
                } catch (IOException e1) {
                    throw new ProgramExceptionExit("pdf load:" + pdfFile.getAbsolutePath(), e1);
                }
            }
        } catch (IOException e) {
            throw new ProgramExceptionExit("pdf load:" + pdfFile.getAbsolutePath(), e);
        }
        return inPDF;
    }

    public Map<Integer, AgentBookmark> identifyBookmarks(PDDocument inPDF) throws ProgramExceptionExit {
        Map<Integer, AgentBookmark> bookmarksByPage = new HashMap<Integer, AgentBookmark>();
        if (inPDF.getDocumentCatalog() == null || inPDF.getDocumentCatalog().getDocumentOutline() == null) {
            return bookmarksByPage;
        }
        PDOutlineItem current = inPDF.getDocumentCatalog().getDocumentOutline().getFirstChild();
        while (current != null) {
            int startPage = -1;
            int endPage = -1;
            try {
                startPage = inPDF.getDocumentCatalog().getPages().indexOf(current.findDestinationPage(inPDF)) + 1;
                if (current.getNextSibling() != null) {
                    endPage = inPDF.getDocumentCatalog().getPages().indexOf(current.getNextSibling().findDestinationPage(inPDF));
                } else {
                    endPage = inPDF.getPages().getCount();
                }
            } catch (IOException e) {
                throw new ProgramExceptionExit("identify bookmarks", e);
            }
            if (startPage >= 0) {
                AgentBookmark agentBookmark = new AgentBookmark(current.getTitle(), startPage, endPage);
                bookmarksByPage.put(agentBookmark.getStartAtPage(), agentBookmark);
            }
            current = current.getNextSibling();
        }
        ui.println("#" + bookmarksByPage.size() + " signets trouvés");
        return bookmarksByPage;
    }

    public List<PDDocument> splitIntoChildFiles(PDDocument inPDF, Map<Integer, AgentBookmark> bookmarksByPage) throws ProgramExceptionExit {
        List<PDDocument> outPdDocuments = new LinkedList<PDDocument>();
        Set<String> viewedFiles = new HashSet<>();
        int duplicatesFound = 0;
        for (AgentBookmark bookmark : bookmarksByPage.values()) {
            try {
                String title = bookmark.getStandardizedTitle();
                String folder = bookmark.getStandardizedFolder();
                String key = title + folder;
                if (viewedFiles.contains(key)) {
                    duplicatesFound++;
                    continue;
                }
                viewedFiles.add(key);
                PDDocument outPdDocument = splitFile(inPDF, bookmark.getStartAtPage(), bookmark.getEndAtPage());
                addMetaData(outPdDocument, title, folder);
                outPdDocuments.add(outPdDocument);
            } catch (IOException e) {
                throw new ProgramExceptionExit("split file by bookmarks", e);
            }
        }
        if (duplicatesFound > 0) {
            ui.errPrintln("!ignored duplicated bookmarks in document: " + duplicatesFound);
        }
        return outPdDocuments;
    }

    /**
     * @param outPdDocument
     * @param fileName      + PDF_EXTENSION will be added
     * @param folder
     */
    private void addMetaData(PDDocument outPdDocument, String fileName, String folder) {
        outPdDocument.getDocumentCatalog().setMetadata(new PDMetadata(outPdDocument));
        outPdDocument.getDocumentCatalog().getMetadata().getCOSObject().setString(DATA_FILENAME, fileName + PDF_EXTENSION);
        outPdDocument.getDocumentCatalog().getMetadata().getCOSObject().setString(DATA_FOLDER, folder);
    }

    private PDDocument splitFile(PDDocument inPDF, int startPage, int endPage) throws IOException {
        Splitter splitter = new Splitter();
        splitter.setStartPage(startPage);
        splitter.setEndPage(endPage);
        splitter.setSplitAtPage(endPage - startPage + 1);
        return splitter.split(inPDF).get(0);
    }

    public void generateGroupedOutputChildPdfs(File toSendFolder, List<PDDocument> outPdDocuments) throws IOException, ProgramExceptionExit {
        ui.println("#génération groupée: " + toSendFolder.getAbsolutePath());
        toSendFolder.mkdir();
        for (PDDocument outPdDocument : outPdDocuments) {
            String fileName = outPdDocument.getDocumentCatalog().getMetadata().getCOSObject().getString(Tool.DATA_FILENAME);
            File outFile = new File(toSendFolder + File.separator + BULLETINS + File.separator + fileName);
            saveFile(outFile, outPdDocument);
        }
    }

    public void generateDispatchedOutputChildPdfs(List<PDDocument> outPdDocuments, File outputFolder, File unknownFolder) throws ProgramExceptionExit, IOException {
        ui.println("#rangement des fichiers");
        for (PDDocument outPdDocument : outPdDocuments) {
            String searchedFolder = outPdDocument.getDocumentCatalog().getMetadata().getCOSObject().getString(Tool.DATA_FOLDER);
            File targetFolder = null;
            if (!searchedFolder.equals(Tool.UNKNOWN_FOLDER)) {
                targetFolder = searchFolder(outputFolder, searchedFolder);
                if (targetFolder == null) {
                    targetFolder = magicSearchFolder(outputFolder, searchedFolder);
                }
            }
            if (targetFolder == null) {
                targetFolder = unknownFolder;
                ui.errPrintln("!" + searchedFolder + " non trouvé");
            } else {
                ui.println(searchedFolder + " trouvé " + targetFolder.getAbsolutePath());
                targetFolder = new File(targetFolder.getAbsolutePath() + File.separator + BULLETINS);
            }
            String fileName = outPdDocument.getDocumentCatalog().getMetadata().getCOSObject().getString(Tool.DATA_FILENAME);
            saveFile(new File(targetFolder + File.separator + fileName), outPdDocument);
        }
        ui.println("").println("#les documents ont été générés et rangés");
        if (unknownFolder.exists() && unknownFolder.list().length > 0) {
            ui.errPrintln("#!" + unknownFolder.getAbsolutePath());
            ui.errPrintln("#!" + unknownFolder.listFiles().length + " documents non rangés (ce traitement ou un précdent)");
            ui.errPrintln("#!rangez manuellement ou supprimez les pour faire disparaitre cette alerte");
        }
    }

    private void saveFile(File fileName, PDDocument outPdDocument) throws IOException, ProgramExceptionExit {
        if (!fileName.getParentFile().exists() && !fileName.getParentFile().mkdirs()) {
            throw new ProgramExceptionExit("save file mkdirs", new IllegalStateException(fileName.getParentFile().getAbsolutePath() + " can't be created"));
        }
        if (fileName.exists()) {
            throw new ProgramExceptionExit("save file", new IllegalStateException(fileName.getAbsolutePath() + " already exists"));
        }
        outPdDocument.save(fileName);
    }

    private File searchFolder(File startFolder, String searchedFolder) {
        for (File folder : startFolder.listFiles(FOLDER_FILTER)) {
            if (searchedFolder.equalsIgnoreCase(folder.getName())) {
                return folder;
            }

            File result = searchFolder(folder, searchedFolder);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private File magicSearchFolder(File startFolder, String searchedFolder) {
        for (File folder : startFolder.listFiles(FOLDER_FILTER)) {
            boolean magicFound = true;
            for (String partWord : searchedFolder.split("[-_ ]")) {
                if (!folder.getName().toLowerCase().contains(partWord.toLowerCase())) {
                    magicFound = false;
                    break;
                }
            }
            if (magicFound) {
                return folder;
            }
            File result = magicSearchFolder(folder, searchedFolder);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

}
