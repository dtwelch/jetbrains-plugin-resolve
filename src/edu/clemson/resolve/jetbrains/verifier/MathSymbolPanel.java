package edu.clemson.resolve.jetbrains.verifier;

import com.intellij.application.options.colors.*;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.colors.ex.DefaultColorSchemesManager;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorMarkupModel;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.impl.FontInfo;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.editor.richcopy.FontMapper;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBDefaultTreeCellRenderer;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.EventDispatcher;
import com.intellij.util.FontUtil;
import com.intellij.util.messages.Topic;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.sun.istack.internal.NotNull;
import org.jdom.Element;
import org.jetbrains.lang.manifest.highlighting.ManifestColorsAndFonts;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Locale;

/**
 * Displays a hierarchy of useful math symbols and the corresponding live template commands to insert them into
 * the active editor.
 */
public class MathSymbolPanel extends JBPanel {

    private final Tree tree;

    public MathSymbolPanel(Project project) {
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("top");
        this.tree = new Tree(top);
        JBDefaultTreeCellRenderer renderer = new JBDefaultTreeCellRenderer(tree);
        renderer.setLeafIcon(null);
        renderer.setClosedIcon(AllIcons.Nodes.NewFolder);
        renderer.setOpenIcon(AllIcons.Nodes.NewFolder);
        renderer.setFont(JBFont.create(new Font(EditorColorsManager.getInstance()
                .getGlobalScheme().getEditorFontName(), Font.PLAIN, 12)));
        tree.setCellRenderer(renderer);
        createSections(top);

        DefaultTreeModel treeModel = new DefaultTreeModel(top);
        this.tree.setModel(treeModel);

        //allows us to change font dynamically in the symbol browser
        //(it matters cause the symbols are utf8 and various fonts render them differently)
        ApplicationManager.getApplication().getMessageBus().connect()
                .subscribe(EditorColorsManager.TOPIC, new EditorColorsListener() {
            @Override
            public void globalSchemeChange(EditorColorsScheme scheme) {
                TreeCellRenderer r = tree.getCellRenderer();
                if (!(r instanceof JBDefaultTreeCellRenderer)) return;
                JBDefaultTreeCellRenderer rAsJBCellRenderer = (JBDefaultTreeCellRenderer)r;
                rAsJBCellRenderer.setFont(JBFont.create(new Font(EditorColorsManager.getInstance()
                        .getGlobalScheme().getEditorFontName(), Font.PLAIN, 12)));
                rAsJBCellRenderer.revalidate();
            }
        });

        //fires when an element is selected
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                if (editor == null) return;
                int tailOffset = editor.getCaretModel().getOffset();
                Document document = editor.getDocument();
                PsiDocumentManager.getInstance(project).commitDocument(document);
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (node == null) return;
                if (!(node.getUserObject() instanceof SymbolInfo)) return;
                SymbolInfo s = (SymbolInfo)node.getUserObject();

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        document.insertString(tailOffset, s.symbol);
                    }
                };
                WriteCommandAction.runWriteCommandAction(project, runnable);
                editor.getCaretModel().moveToOffset(tailOffset + 1);
                tree.clearSelection();
            }
        });

        //Create a tree that allows multiple (non contiguous) expansions at a time
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        this.tree.setRootVisible(false);
        JScrollPane treeView = new JBScrollPane(tree);
        treeView.setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout());

        JTextField ff = new JBTextField("filter");
        //ff.setFont(UIUtil.getTreeFont().co);
        ff.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                ff.setText("");
            }

            public void focusLost(FocusEvent e) {
            }
        });
        ff.setBorder(new RoundedCornerBorder());

        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BorderLayout());
        filterPanel.add(ff, BorderLayout.CENTER);
        filterPanel.setBorder(BorderFactory.createLineBorder(JBColor.WHITE, 4));

        add(filterPanel, BorderLayout.NORTH);
        add(treeView);
    }

    private void createSections(@NotNull DefaultMutableTreeNode e) {
        addArrowsSection(e);
        addGreekSection(e);
        addLettersSection(e);
        //TODO: Add the rest
    }

    private void addArrowsSection(@NotNull DefaultMutableTreeNode e) {
        DefaultMutableTreeNode category = new DefaultMutableTreeNode("Arrows");

        category.add(new DefaultMutableTreeNode(new SymbolInfo("←", "leftarrow")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("⇐", "Leftarrow")));

        category.add(new DefaultMutableTreeNode(new SymbolInfo("⟵", "longleftarrow")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("⟸", "Longleftarrow")));

        category.add(new DefaultMutableTreeNode(new SymbolInfo("→", "rightarrow")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("⇒", "Rightarrow")));

        category.add(new DefaultMutableTreeNode(new SymbolInfo("⟶", "longrightarrow")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("⟹", "Longrightarrow")));

        category.add(new DefaultMutableTreeNode(new SymbolInfo("↔", "leftrightarrow")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("⇔", "Leftrightarrow")));

        category.add(new DefaultMutableTreeNode(new SymbolInfo("⟷", "longleftrightarrow")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("⟺", "Longleftrightarrow")));
        e.add(category);
    }

    private void addGreekSection(@NotNull DefaultMutableTreeNode e) {
        DefaultMutableTreeNode category = new DefaultMutableTreeNode("Greek");

        //lowercase
        category.add(new DefaultMutableTreeNode(new SymbolInfo("α", "alpha")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("β", "beta")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("γ", "gamma")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("δ", "delta")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("ε", "epsilon")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("ζ", "zeta")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("η", "eta")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("θ", "theta")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("ι", "iota")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("κ", "kappa")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("λ", "lambda")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("μ", "mu")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("ν", "nu")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("ξ", "xi")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("ο", "omnicron")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("π", "pi")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("ρ", "rho")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("σ", "sigma")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("τ", "tau")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("υ", "upsilon")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("φ", "phi")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("χ", "chi")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("ψ", "psi")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("ω", "omega")));

        //capitals
        category.add(new DefaultMutableTreeNode(new SymbolInfo("Γ", "Gamma")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("Δ", "Delta")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("Λ", "Lambda")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("Ξ", "Xi")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("Π", "Pi")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("Σ", "Sigma")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("Υ", "Upsilon")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("Φ", "Phi")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("Ψ", "Psi")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("Ω", "Omega")));
        e.add(category);
    }

    private void addLettersSection(@NotNull DefaultMutableTreeNode e) {
        DefaultMutableTreeNode category = new DefaultMutableTreeNode("Letters");
        category.add(new DefaultMutableTreeNode(new SymbolInfo("ℂ", "Complex")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("ℕ", "Natual")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("ℚ", "Rational")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("ℝ", "Real")));
        category.add(new DefaultMutableTreeNode(new SymbolInfo("ℤ", "Integer")));
        e.add(category);
    }

    /** A class for grouping all math glyph related information. */
    private static class SymbolInfo {
        private final String symbol, command;

        SymbolInfo(@NotNull String s, @NotNull String command) {
            this.symbol = s;
            this.command = command;
        }

        @Override
        public String toString() {
            return symbol + "   " + command;
        }
    }

    /**
     * An extension of {@link AbstractBorder} for the symbol/glyph search box embedded
     * at the top of the panel
     */
    static class RoundedCornerBorder extends AbstractBorder {
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int r = height - 1;
            RoundRectangle2D round = new RoundRectangle2D.Float(x, y, width - 1, height - 1, r, r);
            Container parent = c.getParent();
            if (parent != null) {
                g2.setColor(JBColor.WHITE);
                Area corner = new Area(new Rectangle2D.Float(x, y, width, height));
                corner.subtract(new Area(round));
                g2.fill(corner);
            }
            g2.setColor(JBColor.GRAY);
            g2.draw(round);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return JBUI.insets(4, 8);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.right = 8;
            insets.top = insets.bottom = 4;
            return insets;
        }
    }
}
