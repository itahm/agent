package com.itahm.table;

import java.io.File;
import java.io.IOException;

import com.itahm.table.Table;

public class Config extends Table {

	public Config(File dataRoot) throws IOException {
		super(dataRoot, CONFIG);
	}
}
