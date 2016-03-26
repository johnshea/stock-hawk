package com.sam_chordas.android.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;

/**
 * Created by John on 3/20/2016.
 */
public class QuoteWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for(int i = 0; i < appWidgetIds.length; i++) {
            Intent intent = new Intent(context, QuoteWidgetRemoteViewsService.class);

            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_collection);

            rv.setRemoteAdapter(appWidgetIds[i], R.id.widget_list, intent);

            appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
}
