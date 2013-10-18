/*
 * JasperReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2013 Jaspersoft Corporation. All rights reserved.
 * http://www.jaspersoft.com
 *
 * Unless you have purchased a commercial license agreement from Jaspersoft,
 * the following license terms apply:
 *
 * This program is part of JasperReports.
 *
 * JasperReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JasperReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JasperReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.jasperreports.components.headertoolbar.actions;


/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 * @version $Id$
 */
public class EditTextElementData extends BaseColumnData {

    public static final String APPLY_TO_HEADING = "heading";
    public static final String APPLY_TO_DETAIL_ROWS = "detailrows";
    public static final String APPLY_TO_GROUPHEADING = "groupheading";
    public static final String APPLY_TO_GROUP_SUBTOTAL = "groupsubtotal";
    public static final String APPLY_TO_TABLE_TOTAL = "tabletotal";

	private int columnIndex;
	private String headingName;
	private String fontName;
	private String fontSize;
	private Boolean fontBold;
	private Boolean fontItalic;
	private Boolean fontUnderline;
	private String fontColor;
	private String fontBackColor;
	private String fontHAlign;
	private String formatPattern;
	private String mode;

    private String dataType;
    private String applyTo;
    private String groupName;

	public EditTextElementData() {
	}

	public int getColumnIndex() {
		return columnIndex;
	}

	public void setColumnIndex(int columnIndex) {
		this.columnIndex = columnIndex;
	}

	public String getHeadingName() {
		return headingName;
	}

	public void setHeadingName(String headingName) {
		this.headingName = headingName;
	}

	public String getFontName() {
		return fontName;
	}

	public void setFontName(String fontName) {
		this.fontName = fontName;
	}

	public String getFontSize() {
		return fontSize;
	}
	
	public void setFontSize(String fontSize) {
		this.fontSize = fontSize;
	}

	public Boolean getFontBold() {
		return fontBold;
	}

	public void setFontBold(Boolean fontBold) {
		this.fontBold = fontBold;
	}

	public Boolean getFontItalic() {
		return fontItalic;
	}

	public void setFontItalic(Boolean fontItalic) {
		this.fontItalic = fontItalic;
	}

	public Boolean getFontUnderline() {
		return fontUnderline;
	}

	public void setFontUnderline(Boolean fontUnderline) {
		this.fontUnderline = fontUnderline;
	}

	public String getFontColor() {
		return fontColor;
	}

	public void setFontColor(String fontColor) {
		this.fontColor = fontColor;
	}

	public String getFontBackColor() {
		return fontBackColor;
	}

	public void setFontBackColor(String fontBackColor) {
		this.fontBackColor = fontBackColor;
	}

	public String getFontHAlign() {
		return fontHAlign;
	}

	public void setFontHAlign(String fontHAlign) {
		this.fontHAlign = fontHAlign;
	}

	public String getFormatPattern() {
		return formatPattern;
	}

	public void setFormatPattern(String formatPattern) {
		this.formatPattern = formatPattern;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getApplyTo() {
        return applyTo;
    }

    public void setApplyTo(String applyTo) {
        this.applyTo = applyTo;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

}