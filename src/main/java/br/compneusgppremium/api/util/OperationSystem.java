package br.compneusgppremium.api.util;

public class OperationSystem {
    private String getOperatingSystem() {
        String os = System.getProperty("os.name");
        System.out.println("Using System Property: " + os);
        return os;
    }

    public String placeImageSystem(String place) {
        String os = this.getOperatingSystem();
        String[] splitted = os.split(" ");
        os = splitted[0];
        String caminho;
        if ("Mac".equals(os)) {
            caminho = "/Users/my/Pictures/" + place + "/";
        } else if ("Windows".equals(os)) {
            caminho = "C:\\gppremium\\" + place + "\\";
        } else {
            caminho = "/home/sandy/imagens/" + place + "/";
        }
        return caminho;
    }
}
