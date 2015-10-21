/*
This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License version 3 as published by
    the Free Software Foundation.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.cirqwizard.fx.traces;

import javafx.scene.layout.GridPane;
import org.cirqwizard.fx.Context;
import org.cirqwizard.fx.PCBPaneFX;
import org.cirqwizard.fx.SettingsDependentScreenController;
import org.cirqwizard.fx.machining.Machining;
import org.cirqwizard.fx.machining.ToolpathGenerationService;
import org.cirqwizard.fx.machining.TraceMillingToolpathGenerationService;
import org.cirqwizard.gcode.TraceGCodeGenerator;
import org.cirqwizard.layers.TraceLayer;
import org.cirqwizard.post.RTPostprocessor;
import org.cirqwizard.settings.InsulationMillingSettings;
import org.cirqwizard.settings.SettingsFactory;
import org.cirqwizard.settings.ToolSettings;

public abstract class TraceMilling extends Machining
{
    @Override
    protected String getName()
    {
        return "Milling";
    }

    @Override
    protected boolean isEnabled()
    {
        Context context = getMainApplication().getContext();
        return InsertTool.EXPECTED_TOOL.equals(context.getInsertedTool()) &&
                context.getG54X() != null && context.getG54Y() != null && context.getG54Z() != null;
    }

    @Override
    public void populateSettingsGroup(GridPane pane, SettingsDependentScreenController listener)
    {
        pane.getChildren().clear();
        pane.getChildren().add(new TracesSettingsPopOver(getMainApplication().getContext(), this).getView());
    }

    @Override
    public void refresh()
    {
        pcbPane.setToolpathColor(PCBPaneFX.ENABLED_TOOLPATH_COLOR);
        pcbPane.setGerberPrimitives(((TraceLayer)getCurrentLayer()).getElements());

        super.refresh();
    }

    @Override
    protected ToolpathGenerationService getToolpathGenerationService()
    {
        return new TraceMillingToolpathGenerationService(getMainApplication(), overallProgressBar.progressProperty(),
                estimatedMachiningTimeProperty, getCurrentLayer(), getCacheId(), getLayerModificationDate());
    }

    protected abstract boolean mirror();
    protected abstract int getCacheId();
    protected abstract long getLayerModificationDate();

    @Override
    protected String generateGCode()
    {
        InsulationMillingSettings settings = SettingsFactory.getInsulationMillingSettings();
        ToolSettings currentTool = getMainApplication().getContext().getCurrentMillingTool();
        int arcFeed = (currentTool.getFeedXY() * currentTool.getArcs() / 100);
        TraceGCodeGenerator generator = new TraceGCodeGenerator(getMainApplication().getContext(), getCurrentLayer().getToolpaths(), mirror());
        return generator.generate(new RTPostprocessor(), currentTool.getFeedXY(), currentTool.getFeedZ(), arcFeed,
                settings.getClearance().getValue(), settings.getSafetyHeight().getValue(), settings.getWorkingHeight().getValue(),
                currentTool.getSpeed());
    }
}