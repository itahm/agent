package com.itahm.table;

import java.io.File;
import java.io.IOException;

import com.itahm.table.Table;

public class Icon extends Table {

	public Icon(File dataRoot) throws IOException {
		super(dataRoot, ICON);
	}
}
