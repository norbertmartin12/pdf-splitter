package org.pdf_splitter;

public class Main {
    /**
     * Travail\Salaires_Charges_DSN_KPMG_EAM\2020_EAM\3_Mars_2020 >> fichier global<br>
     * Travail\Personnel\Carentan\Angélique >> fichier individu<br>
     * 3350
     */
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
