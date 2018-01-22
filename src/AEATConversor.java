import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Created by eduardo on 17/01/18.
 * Programa que es capaz de interpretar el fichero CSV generado por el Aplicativo Web de AEAT
 * Cuando se solicitan certificados de prestaciones sociales
 *
 * Abre un cuadro de diálogo solicitando el fichero
 * Procesa el CSV y genera un PDF con los expedientes de AEAT
 * Normalmente son 2 páginas por expediente, pero en algunos casos sólo 1 página, en cuyo caso
 * se genera una página adicional para poder imprimir en doble cara
 *
 * Al final del proceso se genera un fichero AEAT.pdf y se abre
 */
public class AEATConversor {

    public static float altura = 0.0f;
    public static float anchura = 0.0f;

    public static void main(String[] args){
        // Creamos la interfaz GUI de selección del Fichero CSV generado por el AEAT
        JFrame frame = new JFrame("SeleccionFichero");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Cuadro de diálgo de selección de fichero
        JFileChooser fileChooserGUI = new JFileChooser();
        frame.add(fileChooserGUI);
        frame.setSize(500, 300);
        int seleccion = fileChooserGUI.showOpenDialog(frame);

        System.out.println(fileChooserGUI.getSelectedFile());

        // Procesamos el fichero seleccionado
        procesaFichero(fileChooserGUI.getSelectedFile().toString());
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new File("AEAT.pdf"));
            }
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }
        frame.dispose();
    }

    private static void procesaCabecera(String linea, Map<String,String> map) {
        // Recoge en un HashMap los campos de cabecera del fichero CSV
        String[] salida = linea.split(";");
        map.put("Tipo Informe",salida[4]);
        map.put("Año",salida[5]);
        map.put("TimeStamp",salida[6]);
        map.put("NIFFuncionario", salida[3]);
    }

    private static void printTitulo (PDPageContentStream contentStream, int posicion, Map<String, String> mapCabecera) throws Exception{
        PDFont font = PDType1Font.HELVETICA_BOLD;
        contentStream.beginText();
        contentStream.setFont(font, 18);
        contentStream.moveTextPositionByAmount(50, AEATConversor.altura - posicion);
        contentStream.drawString(mapCabecera.get("Tipo Informe") + " (" + mapCabecera.get("Año") + ")");
        contentStream.endText();
        contentStream.drawLine(50, AEATConversor.altura - (posicion + 10), AEATConversor.anchura - 50, AEATConversor.altura - (posicion + 10));
    }

    private static void printH1(PDPageContentStream contentStream, int posicion, Map<String, String> mapCabecera, String titulo) throws Exception {
        PDFont font = PDType1Font.HELVETICA_BOLD;
        contentStream.beginText();
        contentStream.setFont(font, 12);
        contentStream.moveTextPositionByAmount(50, altura - posicion);
        contentStream.drawString(titulo);
        contentStream.endText();
        contentStream.drawLine(50, altura - (posicion + 4), anchura - 50, altura - (posicion + 4));
    }

    private static void printCampoH(PDPageContentStream contentStream, int posicion, String campo, String valor) throws Exception {
        PDFont font = PDType1Font.HELVETICA_BOLD;
        PDFont font2 = PDType1Font.HELVETICA;

        contentStream.beginText();
        contentStream.setFont(font, 8);
        contentStream.moveTextPositionByAmount(60, altura - posicion);
        contentStream.drawString(campo);
        contentStream.endText();
        contentStream.beginText();
        contentStream.setFont(font2, 8);
        contentStream.moveTextPositionByAmount(60 + 150, altura - posicion);
        contentStream.drawString(valor);
        contentStream.endText();
    }

    private static void printCampoD(PDPageContentStream contentStream, int posicion, String campo, String valor, int striped) throws Exception {
        PDFont font = PDType1Font.HELVETICA_BOLD;
        PDFont font2 = PDType1Font.HELVETICA;
        // Esto sirve para poner un fondo gris en filas alternas para mejor visualización
        if (striped == 1) {
            contentStream.setNonStrokingColor(240, 240, 240); //fondo gris
            contentStream.fillRect(50, altura - posicion - 5, anchura - 100, 15);
        }
        contentStream.setNonStrokingColor(0, 0, 0); // Texto negro
        contentStream.beginText();
        contentStream.setFont(font, 8);
        contentStream.moveTextPositionByAmount(60, altura - posicion);
        contentStream.drawString(campo);
        contentStream.endText();
        contentStream.beginText();
        contentStream.setFont(font2, 8);
        contentStream.moveTextPositionByAmount(60 + 473, altura - posicion);

        // Esto es para alinear a la izquierda las cifras de cantidad
        float text_width = (font2.getStringWidth(valor) / 1000.0f) * 8;
        contentStream.moveTextPositionByAmount(-text_width, 0);
        contentStream.drawString(valor);
        contentStream.endText();
    }

    private static void printFooter (PDPageContentStream contentStream, int posicion, String texto) throws Exception {
        PDFont font = PDType1Font.HELVETICA;
        contentStream.setNonStrokingColor(0, 0, 0); // Texto negro
        contentStream.beginText();
        contentStream.setFont(font, 8);
        contentStream.moveTextPositionByAmount(80, posicion);
        contentStream.drawString(texto);
        contentStream.endText();
    }

    private static void procesaFichero(String ruta){
        Map<String, String> mapCabecera = new HashMap<String, String>();
        Map<String, String> mapPersona = new HashMap<String, String>();
        try {
            File fichero = new File(ruta);
            List<String> lineas = Files.readAllLines(fichero.toPath(), Charset.forName("ISO-8859-1"));
            // Leemos los campos de cabecera del fichero
            procesaCabecera(lineas.get(1), mapCabecera);

            // Empezamos a crear el documento
            PDDocument documento = new PDDocument();

            for (int i = 4 ; i< lineas.size();i++){
                String[] campos  = lineas.get(i).split(";", -1);
                System.out.println(lineas.get(i));
                System.out.println("Campos: " + campos.length);
                mapPersona.put("Referencia", campos[0]);
                mapPersona.put("Nif", campos[1]);
                mapPersona.put("Nombre-Datos propios", campos[2]);
                mapPersona.put("Nombre identificado", campos[3]);
                mapPersona.put("Resultado", campos[4]);
                mapPersona.put("Tipo de respuesta", campos[5]);
                mapPersona.put("Tipo de contribuyente", campos[6]);
                mapPersona.put("Tipo de declaración", campos[7]);
                mapPersona.put("Código de expediente", campos[8]);
                System.out.println(campos.length);

                //DATOS CONTRIBUYENTE
                // Ponemos la cabecera de la página
                PDPage pagina = new PDPage(PDPage.PAGE_SIZE_A4);
                AEATConversor.altura = pagina.getMediaBox().getHeight();
                AEATConversor.anchura = pagina.getMediaBox().getWidth();

                PDPageContentStream contentStream = new PDPageContentStream(documento, pagina);

                printTitulo(contentStream, 60, mapCabecera);
                printH1(contentStream, 100, mapPersona, "CONTRIBUYENTE");
                printCampoH(contentStream,120,"NIF:",mapPersona.get("Nif") );
                printCampoH(contentStream,140,"APELLIDOS, NOMBRE:",mapPersona.get("Nombre identificado") );
                printCampoH(contentStream,160,"DATOS PROPIOS:",mapPersona.get("Nombre-Datos propios") );
                printH1(contentStream, 190, mapPersona, "CERTIFICADO");
                printCampoH(contentStream,210,"Código identificativo:",mapPersona.get("Resultado") );
                printCampoH(contentStream,230,"Origen datos:",mapPersona.get("Tipo de respuesta") );
                printCampoH(contentStream,250,"Tipo Declaracion:",mapPersona.get("Tipo de declaración") );
                printCampoH(contentStream,270,"Tipo Contribuyente:",mapPersona.get("Tipo de contribuyente") );
                printCampoH(contentStream,290,"Número de importes:","" +  (campos.length - 9));
                printCampoH(contentStream,310,"Referencia",mapPersona.get("Referencia") );
                printH1(contentStream, 340, mapPersona, "IMPORTES");

                // Ponemos length -1 porque el último campo tiene un punto y coma
                int pos = 360; // Posicion de inicio de escritura de campos (calculada a ojo)
                int striped = -1; // Bandas de fondo gris alternas para las filas
                int count = 1;
                int limit = 25; // Máximo número de columnas imprimibles por hoja
                boolean doblepagina = false; // Indica si se ha generado una doble páginas

                String textoFooter = "Estos datos han sido obtendidos por el funcionario " + mapCabecera.get("NIFFuncionario") + " en fecha " + mapCabecera.get("TimeStamp");

                for (int j = 9; j < campos.length - 1; j = j + 2) {
                    if (count == 25){
                        System.out.println("Cerrando pagina 2");
                        doblepagina = true;
                        count = 1;
                        printFooter(contentStream, 90, textoFooter);
                        contentStream.close();
                        documento.addPage(pagina);
                        pagina = new PDPage(PDPage.PAGE_SIZE_A4);
                        contentStream = new PDPageContentStream(documento, pagina);
                        printTitulo(contentStream, 60, mapCabecera);
                        printH1(contentStream, 100, mapPersona, "CONTRIBUYENTE (Página 2)");
                        printCampoH(contentStream,120,"NIF:",mapPersona.get("Nif") );
                        printCampoH(contentStream,140,"APELLIDOS, NOMBRE:",mapPersona.get("Nombre identificado") );
                        printCampoH(contentStream,160,"DATOS PROPIOS:",mapPersona.get("Nombre-Datos propios") );
                        printH1(contentStream, 190, mapPersona, "IMPORTES (Página 2)");
                        pos = 220;
                    }
                    printCampoD(contentStream,pos,campos[j], campos[j+1],striped );
                    striped*=-1;
                    pos+=15;
                    count++;
                    System.out.println("--> " + j +  " " + campos[j] + " : " + campos[j+1]);
                }
                System.out.println("Siguiente registro");
                printFooter(contentStream, 90, textoFooter);
                contentStream.close();
                documento.addPage(pagina);
                // Si no se ha generado 2 hojas, añadimos una hoja en blanco para poder imprimir a doble cara, cada expediente de forma separada.
                if (!doblepagina){
                    pagina = new PDPage(PDPage.PAGE_SIZE_A4);
                    documento.addPage(pagina);
                }
                doblepagina = false;
            }
            // Salvamos el documento al final
            documento.save("AEAT.pdf");
        }
        catch (Exception e){
            System.out.print(e.toString());
        }
    }
}
