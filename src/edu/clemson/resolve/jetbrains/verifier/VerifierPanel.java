package edu.clemson.resolve.jetbrains.verifier;

import com.intellij.codeInsight.preview.ColorPreviewComponent;
import com.intellij.ide.highlighter.JavaHighlightingColors;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.impl.EditorEmptyTextPainter;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.preview.PreviewPanelProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.UIUtil;
import com.sun.istack.internal.NotNull;
import edu.clemson.resolve.jetbrains.RESOLVEFileType;
import edu.clemson.resolve.jetbrains.RESOLVEIcons;
import edu.clemson.resolve.jetbrains.actions.GenerateVCsAction;
import edu.clemson.resolve.proving.Antecedent;
import edu.clemson.resolve.proving.Consequent;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.VC;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

/** Pairs a vc display panel with (optional) derivation info panel */
public class VerifierPanel extends JPanel {

    public static final Logger LOG = Logger.getInstance("RESOLVE VerifierPanel");
    private VCPanelMock activeVcPanel;
    private final Project project;

    public VerifierPanel(Project project) {
        this.project = project;
        //revalidate();
        createBaseGUI();
    }

    public List<VerificationEditorPreview> getActivePreviewEditors() {
        List<VerificationEditorPreview> result = new ArrayList<>();
        if (activeVcPanel != null && activeVcPanel.goalPreview != null) {
            result.add(activeVcPanel.goalPreview);
        }
        return result;
    }

    private void createBaseGUI() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(new EmptyBorder(5, 5, 5, 5));
        String vcShortcut = KeymapUtil.getFirstKeyboardShortcutText("resolve.GenVCs");
        JLabel emptyLabel = new JBLabel(
                "<html>" +
                "<div style='text-align: center;'>" +
                "<font color='#7E7C7B'>" +
                "<b>No Verification Condition (VC)<br>Selected</b>" +
                "<br><br>" +
                "Right-click an open editor and select<br>" +
                "\"RESOLVE Generate VCs\"" +
                "<br>" +
                "(shortcut: <span style=\"color: #7CB5FA\">" + vcShortcut + "</span>)" +
                "<br><br>" +
                "Then left-click one of the VC icons<br>in the gutter to view" +
                "</font>" +
                "</html>", JLabel.CENTER);
        emptyLabel.setFont(createFont(12));
        JPanel dummypanel = new JBPanel();
        dummypanel.setOpaque(false);
        this.add(Box.createRigidArea(new Dimension(0, 50)));
        this.add(emptyLabel);
    }

    public void revertToBaseGUI() {
        if (activeVcPanel != null && activeVcPanel.goalPreview != null) {
            //we're going back to the default screen, so if there were active editors (before say the user messed
            //with the doc) remove em' here.
            //activeVcPanel.goalPreview.removeNotify();
        }
        this.removeAll();
        this.activeVcPanel = null;
        createBaseGUI();
        revalidate();
    }

    public void setActiveVcPanel(VCPanelMock vcp) {
        if (this.activeVcPanel != null && activeVcPanel.goalPreview != null) {
            //before we set the new one, let's try to remember to dispose of the existing one correctly
            //activeVcPanel.goalPreview.disposeUIResources();
            //activeVcPanel.goalPreview.removeNotify();
        }
        this.activeVcPanel = vcp;
        this.removeAll();   // clear any old stuff first
        Splitter splitPane = new Splitter(true);
        splitPane.setFirstComponent(vcp.getComponent());
        splitPane.setSecondComponent(new AssertiveCodeBrowserMock().getComponent());
        this.add(splitPane);
        revalidate();
    }

    public static class VCPanelMock  {
        private VerificationEditorPreview goalPreview, givensPreview = null;

        private JPanel baseComponent;
        private final Project project;

        private final String explanation, goal;
        private final int vcNumber;
        private final List<String> antecedentParts = new ArrayList<>();

        public VCPanelMock(Project project, VC vc) {
            this.project = project;

            this.explanation = vc.getExplanation();
            this.goal = vc.getConsequent().toString();
            for (PExp e : vc.getAntecedent()) {
                antecedentParts.add(e.toString());
            }
            this.vcNumber = vc.getNumber();
            this.baseComponent = createGUI();
        }

        private JPanel createGUI() {
            //WORK 2 below:
            JPanel pane0 = new JBPanel();
            JPanel pane1 = new JBPanel();

            //for the goal box
            JComponent goalComponent = new JPanel();
            goalComponent.setLayout(new BoxLayout(goalComponent, BoxLayout.Y_AXIS));
            goalComponent.setOpaque(true);
            goalComponent.setBackground(JBColor.WHITE);
            goalPreview = new VerificationEditorPreview(project, goal);
            goalPreview.addNotify();

            goalPreview.setMaximumSize(new Dimension(Integer.MAX_VALUE, goalPreview.getPreferredSize().height));
            //goalPreview.setMinimumSize();
            //goalPreview.setMaximumSize(goalPreview.getPreferredSize());  //TODO: This line is a good lead

            //goalComponent.setPreferredSize(goalPreview.getPreferredSize());
            //goalComponent.setPreferredSize(goalComponent.getPreferredSize());

            //TODO: this seems like its going in the right direction
            //goalPreview.setPreferredSize(goalPreview.getPreferredSize());
            //goalComponent.setPreferredSize(new Dimension(50, 4));
            //goalComponent.setPreferredSize(goalComponent.getPreferredSize());

            goalComponent.add(goalPreview, CENTER_ALIGNMENT);

            TitledBorder goalBorder = new TitledBorder(new LineBorder(JBColor.LIGHT_GRAY, 1, true),
                    "<html>" +
                    "<font color='#515151' size='4'>" +
                    "<b>Goal:</b>" +
                    "</font>" +
                    "</html>",
                    TitledBorder.LEFT,
                    TitledBorder.DEFAULT_POSITION);

            goalBorder.setTitleFont(createFont(14));
            goalBorder.setTitleColor(JBColor.BLACK);

            goalComponent.setBorder(goalBorder);

            //for the givens box
            JComponent givensComponent = new JBPanel();
            givensComponent.setLayout(new BoxLayout(givensComponent, BoxLayout.Y_AXIS));
            givensComponent.setOpaque(true);
            givensComponent.setBackground(JBColor.WHITE);
            givensPreview = new VerificationEditorPreview(project, createStringGivensListing());
            givensPreview.addNotify();

            givensPreview.setMaximumSize(new Dimension(Integer.MAX_VALUE, givensPreview.getPreferredSize().height));


            //givensPreview.setPreferredSize(givensPreview.getPreferredSize());
            givensComponent.add(givensPreview, CENTER_ALIGNMENT);
            //givensComponent.setPreferredSize(goalPreview.getPreferredSize());
            //givensComponent.setPreferredSize(givensComponent.getPreferredSize());

            TitledBorder givenBorder = new TitledBorder(new LineBorder(JBColor.LIGHT_GRAY, 1, true),
                    "<html>" +
                    "<font color='#515151' size='4'>" +
                    "<b>Givens:</b>" +
                    "</font>" +
                    "</html>",
                    TitledBorder.LEFT,
                    TitledBorder.DEFAULT_POSITION);
            givensComponent.setBorder(givenBorder);

            JPanel titlePanel = new JBPanel();
            titlePanel.setOpaque(true);
            titlePanel.setBackground(JBColor.WHITE);
            titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.X_AXIS));
            // create and add a label to the temp panel
            String numAndExplanation =
                    "<html>" +
                        "<font color='#4A4F51'>" +
                            "<b>#" + vcNumber + " |</b> "  + explanation +
                        "</font>" +
                    "</html>";
            JLabel label = new JLabel(numAndExplanation);
            label.setIcon(RESOLVEIcons.VC_PANEL);
            titlePanel.add(label);
            // use our stretchy glue to fill the space to the right of the label
            titlePanel.add(Box.createHorizontalGlue());

            pane0.setBorder(new LineBorder(JBColor.LIGHT_GRAY, 1, true));
            pane0.setLayout(new BorderLayout());
            pane0.setOpaque(true);
            pane0.setBackground(JBColor.WHITE);

            pane1.setBorder(new EmptyBorder(4, 4, 4, 4));
            pane1.setLayout(new BoxLayout(pane1, BoxLayout.Y_AXIS));
            pane1.setOpaque(true);
            pane1.setBackground(JBColor.WHITE);

            pane1.add(titlePanel);
            pane1.add(Box.createRigidArea(new Dimension(0, 10)));
            pane1.add(goalComponent);
            pane1.add(Box.createRigidArea(new Dimension(0, 20)));
            pane1.add(givensComponent);
            pane1.add(Box.createRigidArea(new Dimension(0, 10)));

            pane0.add(pane1);
            return pane0;
        }

        private String createStringGivensListing() {
            String formattedGivens = "";
            char count = 'a';
            boolean first = true;
            for (String given : antecedentParts) {
                if (first) {
                    formattedGivens += count + ".)  " + given;
                    first = false;
                }
                else {
                    formattedGivens += "\n" + count + ".)  " + given;
                }
                count++;
            }
            return formattedGivens;
        }
        public JComponent getComponent() {
            return baseComponent;
        }
    }

    public static class AssertiveCodeBrowserMock {
        private final JPanel baseComponent;

        public AssertiveCodeBrowserMock() {
            baseComponent = new JPanel();
            baseComponent.setOpaque(true);
            baseComponent.setBackground(JBColor.WHITE);
            baseComponent.setBorder(new LineBorder(JBColor.LIGHT_GRAY, 1, true));
        }

        public JComponent getComponent() {
            return baseComponent;
        }
    }
    /*
    private static TitledBorder createBorder(String label) {
        TitledBorder result = new TitledBorder(new LineBorder(JBColor.LIGHT_GRAY, 1, true),
                label,
                TitledBorder.LEFT,
                TitledBorder.DEFAULT_POSITION);
        result.setTitleFont(createFont(true, 14));
        result.setTitleColor(JBColor.BLACK);
        return result;
    }
    */
    private static Font createFont(int size) {
        return JBFont.create(new Font(UIUtil.getMenuFont().getName(), Font.PLAIN, size));
    }
}