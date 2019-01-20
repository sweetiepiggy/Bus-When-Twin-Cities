/*
    Copyright (C) 2019 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

    This file is part of Bus When? (Twin Cities).

    Bus When? (Twin Cities) is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    Bus When? (Twin Cities) is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Bus When? (Twin Cities); if not, see <http://www.gnu.org/licenses/>.
*/

package com.sweetiepiggy.buswhentwincities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static final String SOURCE_URL = "https://github.com/sweetiepiggy/Bus-When-Twin-Cities";
    // private CheckStopIdTask mCheckStopIdTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            restoreSavedState(savedInstanceState);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ((EditText) findViewById(R.id.stopIdEntry))
            .setOnEditorActionListener(new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            startStopIdActivity();
                            return true;
                        } else {
                            return false;
                        }
                    }
                });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startStopIdActivity();
                }
            });
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("stopId",
                                     ((EditText) findViewById(R.id.stopIdEntry)).getText().toString());
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoreSavedState(savedInstanceState);
    }

    private void restoreSavedState(Bundle savedInstanceState) {
        String stopId = savedInstanceState.getString("stopId");
        ((EditText) findViewById(R.id.stopIdEntry)).setText(stopId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
        case R.id.action_about:
            intent = new Intent(getApplicationContext(), AboutActivity.class);
            startActivity(intent);
            return true;
        case R.id.action_source:
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(SOURCE_URL), "text/html");
            startActivity(Intent.createChooser(intent, null));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void startStopIdActivity() {
        String stopId = ((EditText) findViewById(R.id.stopIdEntry)).getText().toString();
        if (stopId.length() == 0) {
            ((EditText) findViewById(R.id.stopIdEntry)).setError(getResources().getString(R.string.enter_stop_id));
        } else {
            Intent intent = new Intent(getApplicationContext(), StopIdActivity.class);
            Bundle b = new Bundle();
            b.putString("stopId", stopId);
            intent.putExtras(b);
            startActivity(intent);
        }
    }
}
