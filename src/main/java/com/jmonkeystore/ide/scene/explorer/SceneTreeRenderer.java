package com.jmonkeystore.ide.scene.explorer;

import com.intellij.openapi.util.IconLoader;
import com.jme3.animation.AnimControl;
import com.jme3.animation.SkeletonControl;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class SceneTreeRenderer implements TreeCellRenderer {

    private final int borderSize = 2;

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        Object item = ((DefaultMutableTreeNode) value).getUserObject();
        JLabel label = new JLabel();
        label.setBorder(BorderFactory.createEmptyBorder (borderSize, borderSize, borderSize, borderSize));
        label.setIconTextGap(5);

        if (item instanceof Spatial) {

            Spatial spatial = (Spatial) item;

            String name = (spatial.getName() == null || spatial.getName().trim().isEmpty())
                    ? item.getClass().getSimpleName() + " (no name)"
                    : item.getClass().getSimpleName() + ": " + spatial.getName();

            if (item instanceof Node) {
                label.setIcon(IconLoader.getIcon("/Icons/SceneExplorer/node.gif"));
            }
            else if (item instanceof Geometry) {
                label.setIcon(IconLoader.getIcon("/Icons/SceneExplorer/geometry.gif"));
            }


            // geometry // mesh

            label.setText(name);
        }
        else if (item instanceof Mesh) {
            label.setIcon(IconLoader.getIcon("/Icons/SceneExplorer/mesh.gif"));
            label.setText("Mesh");
        }
        else if (item instanceof Control) {

            label.setText(item.getClass().getSimpleName());

            if (item instanceof AnimControl) {
                label.setIcon(IconLoader.getIcon("/Icons/SceneExplorer/animationControl.gif"));
            }
            else if (item instanceof SkeletonControl) {
                label.setIcon(IconLoader.getIcon("/Icons/SceneExplorer/skeletonControl.gif"));
            }

        }
        else {
            label.setText("Unknown: " + item.getClass().getName());
        }

        return label;
    }

}
