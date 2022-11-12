package org.pdf_splitter;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        Package mainPackage = Main.class.getPackage();
        String version = mainPackage.getImplementationVersion();
        if (version == null) {
            version = "local";
        }
        String artifactId = mainPackage.getImplementationTitle();
        if (artifactId == null) {
            artifactId = "local";
        }
        Controler controler = new Controler(new UI(artifactId + "-" + version));
        controler.launch();
    }

}
