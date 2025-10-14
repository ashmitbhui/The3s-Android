package in.appoholics.the3s;

import android.app.Activity;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfGenerator {

    public interface PdfGenerationListener {
        void onSuccess(Uri pdfUri);

        void onFailure(Exception e);
    }

    private static class PageHeaderEventHandler implements IEventHandler {
        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdfDoc = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            Rectangle pageSize = page.getPageSize();
            PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdfDoc);

            pdfCanvas.release();
        }
    }

    public static void createCatalogue(Activity activity, List<Product> selectedProducts,boolean showPrices, String filename, PdfGenerationListener listener) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                File pdfFile = new File(activity.getCacheDir(), filename + ".pdf");
                PdfWriter writer = new PdfWriter(pdfFile);
                PdfDocument pdfDocument = new PdfDocument(writer);
                pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, new PageHeaderEventHandler());
                Document document = new Document(pdfDocument);

                // Group products by category
                Map<String, List<Product>> groupedByCategory = new HashMap<>();
                for (Product product : selectedProducts) {
                    groupedByCategory.computeIfAbsent(product.getCategory(), k -> new ArrayList<>()).add(product);
                }
                List<String> sortedCategories = new ArrayList<>(groupedByCategory.keySet());
                Collections.sort(sortedCategories);

                boolean isFirstCategory = true;
                for (String category : sortedCategories) {
                    if (!isFirstCategory) {
                        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                    }
                    isFirstCategory = false;

                    document.add(new Paragraph(category)
                            .setBold().setFontSize(18).setMarginTop(20).setMarginBottom(10));

                    Table table = new Table(UnitValue.createPercentArray(3)).useAllAvailableWidth();

                    for (Product product : groupedByCategory.get(category)) {
                        Cell cell = new Cell().setBorder(null);

                        // Add Image
                        try {
                            File imageFile = Glide.with(activity).asFile().load(product.getImageUrl()).submit().get();
                            Image pdfImage = new Image(ImageDataFactory.create(imageFile.getAbsolutePath()));
                            pdfImage.setWidth(UnitValue.createPercentValue(100));
                            cell.add(pdfImage);
                        } catch (Exception e) {
                            cell.add(new Paragraph("(Image not available)").setFontSize(8));
                        }

                        // Add Name, Price, and Detail
                        cell.add(new Paragraph(product.getName()).setBold().setFontSize(10));

                        if (showPrices) {
                            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
                            currencyFormat.setMaximumFractionDigits(0);
                            cell.add(new Paragraph("Rs. "+currencyFormat.format(product.getPrice())).setFontSize(9));
                        }
                        cell.add(new Paragraph(product.getDetail()).setFontSize(8).setItalic());

                        table.addCell(cell);
                    }
                    document.add(table);
                }

                document.close();

                Uri pdfUri = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".fileprovider", pdfFile);
                handler.post(() -> listener.onSuccess(pdfUri));

            } catch (Exception e) {
                handler.post(() -> listener.onFailure(e));
            }
        });
    }
}