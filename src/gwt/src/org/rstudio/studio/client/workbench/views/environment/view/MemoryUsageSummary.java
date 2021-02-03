/*
 * MemoryUsageSummary.java
 *
 * Copyright (C) 2021 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.environment.view;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.MiniPieWidget;
import org.rstudio.studio.client.workbench.views.environment.model.MemoryStat;
import org.rstudio.studio.client.workbench.views.environment.model.MemoryUsage;
import org.rstudio.studio.client.workbench.views.environment.model.MemoryUsageReport;

public class MemoryUsageSummary extends Composite
{
   interface Style extends CssResource
   {
      String kbCell();
      String stats();
      String header();
   }

   /**
    * Creates a new memory usage summary widget
    *
    * @param report The memory usage report to show in the widget.
    */
   public MemoryUsageSummary(MemoryUsageReport report)
   {
      pie_ = new MemoryUsagePieChart(report.getSystemUsage());

      initWidget(GWT.<MemoryUsageSummary.Binder>create(MemoryUsageSummary.Binder.class).createAndBindUi(this));

      MemoryUsage usage = report.getSystemUsage();

      // Size pie chart showing the percentage of memory used
      pie_.getElement().getStyle().setWidth(150, com.google.gwt.dom.client.Style.Unit.PX);
      pie_.getElement().getStyle().setHeight(150, com.google.gwt.dom.client.Style.Unit.PX);
      ElementIds.assignElementId(pie_, ElementIds.MEMORY_PIE_FULL);

      int percent = usage.getPercentUsed();
      pieLabel_.setText(percent + "%");

      Element statsTable = Document.get().createTableElement();
      ElementIds.assignElementId(statsTable, ElementIds.MEMORY_USAGE_TABLE);
      statsTable.setClassName(style.stats());

      // Create the header title for the table
      HeadingElement header = Document.get().createHElement(1);
      header.setClassName(style.header());
      header.setInnerText("Memory Usage");
      stats_.getElement().appendChild(header);
      ElementIds.assignElementId(header, ElementIds.MEMORY_TABLE_TITLE);

      // Create the header row for the table:
      //
      // Statistic  Memory  Source
      // ---------  ------  ------
      Element statsHeader = Document.get().createTHeadElement();
      Element statsRow = Document.get().createTRElement();
      Element statCell = Document.get().createTHElement();
      statCell.setInnerText("Statistic");
      statsRow.appendChild(statCell);
      Element memoryCell = Document.get().createTHElement();
      memoryCell.setInnerText("Memory");
      statsRow.appendChild(memoryCell);
      Element sourceCell = Document.get().createTHElement();
      sourceCell.setInnerText("Source");
      statsRow.appendChild(sourceCell);
      statsHeader.appendChild(statsRow);

      statsTable.appendChild(statsHeader);

      // Create the table body
      Element statsBody = Document.get().createTBodyElement();
      statsTable.appendChild(statsBody);

      // Create a row for each statistic
      statsBody.appendChild(buildStatsRow(
         "Total used by R objects",
         report.getRUsage().getConsKb() + report.getRUsage().getVectorKb(),
         "R"
      ));

      statsBody.appendChild(buildStatsRow(
         "Total used by session",
         report.getSystemUsage().getProcess()));

      statsBody.appendChild(buildStatsRow(
         "Total used by system",
         report.getSystemUsage().getUsed()));

      statsBody.appendChild(buildStatsRow(
         "Total system memory",
         report.getSystemUsage().getTotal()));

      stats_.getElement().appendChild(statsTable);
      Roles.getDialogRole().setAriaLabelledbyProperty(statsTable, Id.of(header));

      // Hack to force SVG to draw
      String html = pie_.getParent().getElement().getInnerHTML();
      pie_.getParent().getElement().setInnerHTML(html);
   }

   public interface Binder extends UiBinder<Widget, MemoryUsageSummary>
   {
   }

   /**
    * Create a row from a memory statistic
    *
    * @param name The name of the statistic
    * @param stat The statistic
    * @return A table row containing the statistic.
    */
   private TableRowElement buildStatsRow(String name, MemoryStat stat)
   {
      return buildStatsRow(name, stat.getKb(), stat.getProviderName());
   }

   /**
    * Create a row from a memory statistic
    *
    * @param stat The name of the statistic
    * @param kb The number of kb in the statistic
    * @param source The source of the statistic
    * @return A table row containing the statistic
    */
   private TableRowElement buildStatsRow(String stat, int kb, String source)
   {
      TableRowElement row = Document.get().createTRElement();

      TableCellElement statCell = Document.get().createTDElement();
      statCell.setInnerText(stat);
      row.appendChild(statCell);

      TableCellElement kbCell = Document.get().createTDElement();
      Element kbVal = Document.get().createElement("strong");
      kbVal.setInnerText(StringUtil.prettyFormatNumber(kb));
      kbCell.appendChild(kbVal);
      Element kbLabel = Document.get().createSpanElement();
      kbLabel.setInnerText(" KiB");
      kbCell.appendChild(kbLabel);
      kbCell.setClassName(style.kbCell());
      row.appendChild(kbCell);

      TableCellElement sourceCell = Document.get().createTDElement();
      sourceCell.setInnerText(source);
      row.appendChild(sourceCell);

      return row;
   }

   @UiField(provided = true) MemoryUsagePieChart pie_;
   @UiField Label pieLabel_;
   @UiField HTMLPanel stats_;
   @UiField Style style;
}
