/*
 * Copyright 2013 must-be.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.consulo.diagram.builder.impl;

import com.intellij.util.containers.HashMap;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;
import org.consulo.diagram.builder.GraphBuilder;
import org.consulo.diagram.builder.GraphNode;
import org.consulo.diagram.builder.GraphPositionStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 22:38/15.10.13
 */
public class GraphBuilderImpl implements GraphBuilder {
  private List<GraphNode<?>> myGraphNodes = new ArrayList<GraphNode<?>>();

  public GraphBuilderImpl() {

  }

  @NotNull
  @Override
  public JComponent getComponent() {
    mxGraph graph = new mxGraph();
    graph.setCellsResizable(false);
    graph.setEdgeLabelsMovable(false);
    graph.setConnectableEdges(false);
    graph.setCellsEditable(false);
    graph.setAllowDanglingEdges(false);

    mxGraphComponent mxGraphComponent = new mxGraphComponent(graph){
      @Override
      public void addNotify() {
        super.addNotify();
        build(this);
      }
    };
    mxGraphComponent.setGridVisible(true);

    return mxGraphComponent;
  }

  private void build(mxGraphComponent mxGraphComponent) {
    Container componentParent = mxGraphComponent.getParent();
    mxGraph graph = mxGraphComponent.getGraph();

    Object parent = graph.getDefaultParent();


    graph.getModel().beginUpdate();
    try
    {
      Map<GraphNode<?>, Object> map = new HashMap<GraphNode<?>, Object>();

      int i = 0;
      for (GraphNode<?> graphNode : myGraphNodes) {
        mxCell vertex = graph.insertVertex(parent, graphNode.getName(), graphNode, 0, i * 100, 0, 0);

        map.put(graphNode, vertex);
        i++;
      }

      for (Map.Entry<GraphNode<?>, Object> entry : map.entrySet()) {
        List<GraphNode<?>> arrowNodes = entry.getKey().getArrowNodes();

        for (GraphNode<?> arrowNode : arrowNodes) {
          mxCell mxCell = graph.insertEdge(parent, null, null, entry.getValue(), map.get(arrowNode));
          mxCell.setStyle(mxConstants.STYLE_STROKECOLOR + "=" + "#FF0000");
        }
      }
    }
    finally
    {
      graph.getModel().endUpdate();
    }
  }

  @NotNull
  @Override
  public <E> GraphNode<E> createNode(@NotNull String name, @Nullable Icon icon, @Nullable E value, GraphPositionStrategy strategy) {
    GraphNodeImpl<E> graphNode = new GraphNodeImpl<E>(name, icon, value, strategy);
    myGraphNodes.add(graphNode);
    return graphNode;
  }
}
