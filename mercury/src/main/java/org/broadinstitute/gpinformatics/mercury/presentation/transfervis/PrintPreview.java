package org.broadinstitute.gpinformatics.mercury.presentation.transfervis;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

/**
 * A dialog for print preview of graphs
 */
public class PrintPreview extends JFrame implements ActionListener {

        public PrintPreview(Component pc) {

            super("Print Preview"/*, true, true, true, true*/);

            printableComponent = pc;

// buttons
            portraitRB = new JRadioButton("Portrait");
            portraitRB.setActionCommand("1");
            portraitRB.setSelected(true);

            landscapeRB = new JRadioButton("Landscape");
            landscapeRB.setActionCommand("2");


            pageFormatBG.add(portraitRB);
            pageFormatBG.add(landscapeRB);

            portraitRB.setBackground(Color.WHITE);
            landscapeRB.setBackground(Color.WHITE);
            constrainProportionsCB.setBackground(Color.WHITE);


            preview.addActionListener(this);
            print.addActionListener(this);


// set
            previewPage.setPreferredSize(new Dimension(460, 460));
            previewPage.setBorder(BorderFactory.createLineBorder(Color.black, 2));
            previewPage.setBackground(Color.WHITE);


            hold.setPreferredSize(new Dimension(200, 280));
            hold.setBorder(BorderFactory.createLineBorder(Color.black, 2));
            hold.setBackground(Color.WHITE);


// make main panel
            mainPanel.setBackground(Color.WHITE);

            GridBagLayout gridbag = new GridBagLayout();
            GridBagConstraints constraints = new GridBagConstraints();

            mainPanel.setLayout(gridbag);

// make hold panel
            GridBagLayout g1 = new GridBagLayout();
            GridBagConstraints c1 = new GridBagConstraints();

            hold.setLayout(g1);

// DELETE
            pageFormat.setOrientation(PageFormat.PORTRAIT);

// GUI
// hold
            c1.insets.top = 5;
            c1.insets.left = 45;
            c1.insets.right = 5;

            buildConstraints(c1, 0, 2, 2, 1, 1, 1);
            c1.fill = GridBagConstraints.BOTH;
            g1.setConstraints(portraitRB, c1);

            hold.add(portraitRB);

            c1.insets.top = 2;

            buildConstraints(c1, 0, 4, 2, 1, 1, 1);
            c1.fill = GridBagConstraints.BOTH;
            g1.setConstraints(landscapeRB, c1);

            hold.add(landscapeRB);

            c1.insets.left = 5;
            c1.insets.right = 0;
            c1.insets.top = 25;

            buildConstraints(c1, 0, 6, 1, 1, 1, 1);
            c1.fill = GridBagConstraints.BOTH;
            g1.setConstraints(xsl, c1);

            hold.add(xsl);

            c1.insets.left = 0;
            c1.insets.right = 5;

            buildConstraints(c1, 1, 6, 1, 1, 1, 1);
            c1.fill = GridBagConstraints.BOTH;
            g1.setConstraints(xs, c1);

            hold.add(xs);

            c1.insets.left = 5;
            c1.insets.right = 0;
            c1.insets.top = 5;

            buildConstraints(c1, 0, 8, 1, 1, 1, 1);
            c1.fill = GridBagConstraints.BOTH;
            g1.setConstraints(ysl, c1);

            hold.add(ysl);

            c1.insets.left = 0;
            c1.insets.right = 5;

            buildConstraints(c1, 1, 8, 1, 1, 1, 1);
            c1.fill = GridBagConstraints.BOTH;
            g1.setConstraints(ys, c1);

            hold.add(ys);

            c1.insets.left = 25;
            c1.insets.right = 5;

            buildConstraints(c1, 0, 10, 2, 1, 1, 1);
            c1.fill = GridBagConstraints.BOTH;
            g1.setConstraints(constrainProportionsCB, c1);

            hold.add(constrainProportionsCB);

            c1.insets.left = 35;
            c1.insets.right = 35;
            c1.insets.top = 20;

            buildConstraints(c1, 0, 12, 2, 1, 1, 1);
            c1.fill = GridBagConstraints.BOTH;
            g1.setConstraints(fitToPageB, c1);

            hold.add(fitToPageB);

            c1.insets.left = 35;
            c1.insets.right = 35;
            c1.insets.top = 25;

            buildConstraints(c1, 0, 14, 2, 1, 1, 1);
            c1.fill = GridBagConstraints.BOTH;
            g1.setConstraints(preview, c1);

            hold.add(preview);

            c1.insets.bottom = 15;
            c1.insets.top = 5;

            buildConstraints(c1, 0, 16, 2, 1, 1, 1);
            c1.fill = GridBagConstraints.BOTH;
            g1.setConstraints(print, c1);

            hold.add(print);

            constraints.insets.top = 10;
            constraints.insets.left = 10;
            constraints.insets.bottom = 10;

            buildConstraints(constraints, 0, 2, 1, 1, 1, 1);
            constraints.fill = GridBagConstraints.NONE;
            gridbag.setConstraints(hold, constraints);

            mainPanel.add(hold);

            constraints.insets.right = 10;

            buildConstraints(constraints, 1, 2, 1, 1, 1, 1);
            constraints.fill = GridBagConstraints.NONE;
            gridbag.setConstraints(previewPage, constraints);

            mainPanel.add(previewPage);

// display
            this.setContentPane(mainPanel);

            this.setVisible(true);
            this.setResizable(false);

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            setSize(720, 520);

            Dimension windowSize = getSize();
            setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
                    (Math.max(0, (screenSize.height - windowSize.height) / 2)));
        }

        public void buildConstraints(GridBagConstraints gbc, int gx, int gy, int gw, int gh, double wx, double wy) {
            gbc.gridx = gx;
            gbc.gridy = gy;
            gbc.gridwidth = gw;
            gbc.gridheight = gh;
            gbc.weightx = wx;
            gbc.weighty = wy;
        }

        public class PreviewPage extends JPanel {

            public PreviewPage() {

            }

            public void paint(Graphics g) {
                super.paint(g);
//                Graphics2D g2 = (Graphics2D) g;

// PORTRAIT
                if (pageFormat.getOrientation() == PageFormat.PORTRAIT) {

                    g.setColor(Color.black);
                    g.drawRect(60, 10, 340, 440);

                    int x1 = (int) Math.rint(((double) pageFormat.getImageableX() / 72) * 40);
                    int y1 = (int) Math.rint(((double) pageFormat.getImageableY() / 72) * 40);

                    int l1 = (int) Math.rint(((double) pageFormat.getImageableWidth() / 72) * 40);
                    int h1 = (int) Math.rint(((double) pageFormat.getImageableHeight() / 72) * 40);

                    g.setColor(Color.red);
                    g.drawRect(x1 + 60, y1 + 10, l1, h1);

                    setScales();

                    int x2 = (int) Math.rint(1028 / XSC);
                    int y2 = (int) Math.rint(768 / YSC);


                    Image image = new BufferedImage(x2, y2, BufferedImage.TYPE_INT_ARGB);

                    printableComponent.paint(image.getGraphics());

                    g.drawImage(image, x1 + 60, y1 + 10, l1, h1, this);


                }

// LANDSCAPE
                if (pageFormat.getOrientation() == PageFormat.LANDSCAPE) {

                    g.setColor(Color.black);
                    g.drawRect(10, 60, 440, 340);

                    int x1 = (int) Math.rint(((double) pageFormat.getImageableX() / 72) * 40);
                    int y1 = (int) Math.rint(((double) pageFormat.getImageableY() / 72) * 40);

                    int l1 = (int) Math.rint(((double) pageFormat.getImageableWidth() / 72) * 40);
                    int h1 = (int) Math.rint(((double) pageFormat.getImageableHeight() / 72) * 40);

                    g.setColor(Color.red);
                    g.drawRect(x1 + 10, y1 + 60, l1, h1);

                    setScales();

                    int x2 = (int) Math.rint(1028 / XSC);
                    int y2 = (int) Math.rint(768 / YSC);


                    Image image = new BufferedImage(x2, y2, BufferedImage.TYPE_INT_ARGB);

                    printableComponent.paint(image.getGraphics());

                    g.drawImage(image, x1 + 10, y1 + 60, l1, h1, this);
                }
            }
        }

        public void actionPerformed(ActionEvent e) {
// fit to page
            if (e.getSource() == fitToPageB) {

            }

// preview
            if (e.getSource() == preview) {
                setProperties();
            }

// print
            if (e.getSource() == print) {
                doPrint();
            }
        }

        public void setProperties() {

            if (portraitRB.isSelected()) {
                pageFormat.setOrientation(PageFormat.PORTRAIT);
            }

            if (landscapeRB.isSelected()) {
                pageFormat.setOrientation(PageFormat.LANDSCAPE);
            }

            setScales();

            previewPage.repaint();
        }

        public void setScales() {

            try {
                XSC = Double.parseDouble(xs.getText());
            } catch (NumberFormatException e) {
            }

            try {
                YSC = Double.parseDouble(ys.getText());
            } catch (NumberFormatException e) {
            }
        }

        public void doPrint() {
            PrintThis();
        }

        public void PrintThis() {
            PrinterJob printerJob = PrinterJob.getPrinterJob();

            Book book = new Book();
            book.append(new PrintPage(), pageFormat);

            printerJob.setPageable(book);

            boolean doPrint = printerJob.printDialog();
            if (doPrint) {
                try {
                    printerJob.print();
                } catch (PrinterException exception) {
                    System.err.println("Printing error: " + exception);
                }
            }
        }

        public class PrintPage implements Printable {

            public int print(Graphics g, PageFormat format, int pageIndex) {
                Graphics2D g2D = (Graphics2D) g;

                g2D.translate(format.getImageableX(), format.getImageableY());

                disableDoubleBuffering(mainPanel);

                System.out.println("get i x " + format.getImageableX());
                System.out.println("get i x " + format.getImageableY());
                System.out.println("getx: " + format.getImageableWidth());
                System.out.println("getx: " + format.getImageableHeight());


                // scale to fill the page
                double dw = format.getImageableWidth();
                double dh = format.getImageableHeight();
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

                setScales();

                double xScale = dw / (1028 / XSC);
                double yScale = dh / (768 / YSC);
                double scale = Math.min(xScale, yScale);


                System.out.println("" + scale);

                g2D.scale(xScale, yScale);


                printableComponent.paint(g);

                enableDoubleBuffering(mainPanel);


                return Printable.PAGE_EXISTS;
            }

            public void disableDoubleBuffering(Component c) {
                RepaintManager currentManager = RepaintManager.currentManager(c);
                currentManager.setDoubleBufferingEnabled(false);
            }

            public void enableDoubleBuffering(Component c) {
                RepaintManager currentManager = RepaintManager.currentManager(c);
                currentManager.setDoubleBufferingEnabled(true);
            }
        }

        Component printableComponent;

        PageFormat pageFormat = new PageFormat();

        double XSC = 1;
        double YSC = 1;

// gui comp
        JPanel mainPanel = new JPanel();
        JPanel hold = new JPanel();
        JPanel previewPage = new PreviewPage();

        ButtonGroup pageFormatBG = new ButtonGroup();
        JRadioButton portraitRB;
        JRadioButton landscapeRB;

        JLabel xsl = new JLabel("X Scale:", JLabel.LEFT);
        JTextField xs = new JTextField("1");

        JLabel ysl = new JLabel("Y Scale:", JLabel.LEFT);
        JTextField ys = new JTextField("1");

        JButton fitToPageB = new JButton("Fit to Page");
        JCheckBox constrainProportionsCB = new JCheckBox("Constrain Proportions");

        JButton preview = new JButton("PREVIEW");
        JButton print = new JButton("PRINT");
    }
