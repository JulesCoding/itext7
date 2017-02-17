package com.itextpdf.layout.renderer;

import com.itextpdf.io.LogMessageConstant;
import com.itextpdf.layout.border.Border;
import com.itextpdf.layout.property.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CollapsedTableBorders extends TableBorders {
    private List<Border> topBorderCollapseWith;
    private List<Border> bottomBorderCollapseWith;


    // region constructors

    public CollapsedTableBorders(List<CellRenderer[]> rows, int numberOfColumns) {
        super(rows, numberOfColumns);

    }

    public CollapsedTableBorders(List<CellRenderer[]> rows, int numberOfColumns, Border[] tableBoundingBorders) {
        super(rows, numberOfColumns, tableBoundingBorders);
        topBorderCollapseWith = new ArrayList<Border>();
        bottomBorderCollapseWith = new ArrayList<Border>();
    }

    public CollapsedTableBorders(List<CellRenderer[]> rows, int numberOfColumns, Border[] tableBoundingBorders, boolean isLargeTable) {
        this(rows, numberOfColumns, tableBoundingBorders);
        this.isLargeTable = isLargeTable;
    }


    // endregion

    // region collapsing and correction

    protected CollapsedTableBorders collapseAllBordersAndEmptyRows(List<CellRenderer[]> rows, Border[] tableBorders, int startRow, int finishRow) {
        CellRenderer[] currentRow;
        int[] rowsToDelete = new int[numberOfColumns];
        for (int row = startRow; row <= finishRow; row++) {
            currentRow = rows.get(row);
            boolean hasCells = false;
            for (int col = 0; col < numberOfColumns; col++) {
                if (null != currentRow[col]) {
                    int colspan = (int) currentRow[col].getPropertyAsInteger(Property.COLSPAN);
                    prepareBuildingBordersArrays(currentRow[col], tableBorders, numberOfColumns, row, col);
                    buildBordersArrays(currentRow[col], row, col);
                    hasCells = true;
                    if (rowsToDelete[col] > 0) {
                        int rowspan = (int) currentRow[col].getPropertyAsInteger(Property.ROWSPAN) - rowsToDelete[col];
                        if (rowspan < 1) {
                            Logger logger = LoggerFactory.getLogger(TableRenderer.class);
                            logger.warn(LogMessageConstant.UNEXPECTED_BEHAVIOUR_DURING_TABLE_ROW_COLLAPSING);
                            rowspan = 1;
                        }
                        currentRow[col].setProperty(Property.ROWSPAN, rowspan);
                    }
                    for (int i = 0; i < colspan; i++) {
                        rowsToDelete[col + i] = 0;
                    }
                    col += colspan - 1;
                } else {
                    if (horizontalBorders.get(row).size() <= col) {
                        horizontalBorders.get(row).add(null);
                    }
                }
            }
            if (!hasCells) {
                rows.remove(currentRow);
                row--;
                finishRow--;
                for (int i = 0; i < numberOfColumns; i++) {
                    rowsToDelete[i]++;
                }
                if (row == finishRow) {
                    Logger logger = LoggerFactory.getLogger(TableRenderer.class);
                    logger.warn(LogMessageConstant.LAST_ROW_IS_NOT_COMPLETE);
                }
            }
        }
        return this;
    }

    // endregion

    // region intializers



    //endregion

    // region getters

    public float[] getCellBorderIndents(int row, int col, int rowspan, int colspan, boolean forceNotToProcessAsLast) {
        float[] indents = new float[4];
        List<Border> borderList;
        Border border;
        // process top border
        borderList = getHorizontalBorder(rowRange.getStartRow() + row - rowspan + 1);
        for (int i = col; i < col + colspan; i++) {
            border = borderList.get(i);
            if (null != border && border.getWidth() > indents[0]) {
                indents[0] = border.getWidth();
            }
        }
        // process right border
        borderList = getVerticalBorder(col + colspan);
        for (int i = rowRange.getStartRow() + row - rowspan + 1; i < rowRange.getStartRow() + row + 1; i++) {
            border = borderList.get(i);
            if (null != border && border.getWidth() > indents[1]) {
                indents[1] = border.getWidth();
            }
        }
        // process bottom border
        borderList = getHorizontalBorder(rowRange.getStartRow() + row + 1, false, forceNotToProcessAsLast);
        for (int i = col; i < col + colspan; i++) {
            border = borderList.get(i);
            if (null != border && border.getWidth() > indents[2]) {
                indents[2] = border.getWidth();
            }
        }
        // process left border
        borderList = getVerticalBorder(col);
        for (int i = rowRange.getStartRow() + row - rowspan + 1; i < rowRange.getStartRow() + row + 1; i++) {
            border = borderList.get(i);
            if (null != border && border.getWidth() > indents[3]) {
                indents[3] = border.getWidth();
            }
        }
        return indents;
    }

    public float[] getCellBorderIndents(int row, int col, int rowspan, int colspan) {
        return getCellBorderIndents(row, col, rowspan, colspan, false);
    }

    public Border getWidestHorizontalBorder(int row, boolean forceNotToProcessAsFirst, boolean forceNotToProcessWithLast) {
        Border theWidestBorder = null;
        if (row >= 0 && row < horizontalBorders.size()) {
            theWidestBorder = getWidestBorder(getHorizontalBorder(row, forceNotToProcessAsFirst, forceNotToProcessWithLast));
        }
        return theWidestBorder;
    }

    public Border getWidestHorizontalBorder(int row, int start, int end, boolean forceNotToProcessAsFirst, boolean forceNotToProcessAsLast) {
        Border theWidestBorder = null;
        if (row >= 0 && row < horizontalBorders.size()) {
            theWidestBorder = getWidestBorder(getHorizontalBorder(row, forceNotToProcessAsFirst, forceNotToProcessAsLast), start, end);
        }
        return theWidestBorder;
    }

    public Border getWidestHorizontalBorder(int row, int start, int end) {
        return getWidestHorizontalBorder(row, start, end, false, false);
    }

    public Border getWidestVerticalBorder(int col) {
        Border theWidestBorder = null;
        if (col >= 0 && col < verticalBorders.size()) {
            theWidestBorder = getWidestBorder(getVerticalBorder(col));
        }
        return theWidestBorder;
    }

    public Border getWidestVerticalBorder(int col, int start, int end) {
        Border theWidestBorder = null;
        if (col >= 0 && col < verticalBorders.size()) {
            theWidestBorder = getWidestBorder(getVerticalBorder(col), start, end);
        }
        return theWidestBorder;
    }

    public List<Border> getVerticalBorder(int index) { // TODO REFACTOR
        if (index == 0) {
            List<Border> borderList = getBorderList(null, tableBoundingBorders[3], verticalBorders.get(0).size());
            List<Border> leftVerticalBorder = verticalBorders.get(0);
            for (int i = 0; i < leftVerticalBorder.size(); i++) {
                if (null == borderList.get(i) || (null != leftVerticalBorder.get(i) && leftVerticalBorder.get(i).getWidth() > borderList.get(i).getWidth())) {
                    borderList.set(i, leftVerticalBorder.get(i));
                }
            }
            return borderList;
        } else if (index == numberOfColumns) {
            List<Border> borderList = getBorderList(null, tableBoundingBorders[1], verticalBorders.get(0).size());
            List<Border> rightVerticalBorder = verticalBorders.get(verticalBorders.size() - 1);
            for (int i = 0; i < rightVerticalBorder.size(); i++) {
                if (null == borderList.get(i) || (null != rightVerticalBorder.get(i) && rightVerticalBorder.get(i).getWidth() > borderList.get(i).getWidth())) {
                    borderList.set(i, rightVerticalBorder.get(i));
                }
            }
            return borderList;
        } else {
            return verticalBorders.get(index);
        }
    }

    public float getMaxTopWidth(boolean forceNoToProcessAsFirst) {
        float width = 0;
        Border widestBorder = getWidestHorizontalBorder(rowRange.getStartRow(), forceNoToProcessAsFirst, false);
        if (null != widestBorder && widestBorder.getWidth() >= width) {
            width = widestBorder.getWidth();
        }
        return width;
    }

    public float getMaxBottomWidth(boolean forceNoToProcessAsLast) {
        float width = 0;
        Border widestBorder = getWidestHorizontalBorder(rowRange.getFinishRow() + 1, false, forceNoToProcessAsLast); // TODO
        if (null != widestBorder && widestBorder.getWidth() >= width) {
            width = widestBorder.getWidth();
        }
        return width;
    }

    public List<Border> getHorizontalBorder(int index) {
        return getHorizontalBorder(index, false, false);
    }

    public List<Border> getHorizontalBorder(int index, boolean forceNotToProcessAsFirst, boolean forceNotToProcessAsLast) {
        if (index == rowRange.getStartRow() && !forceNotToProcessAsFirst) {
            List<Border> firstBorderOnCurrentPage = getBorderList(topBorderCollapseWith, tableBoundingBorders[0], numberOfColumns);
            if (0 != rows.size()) {
                int col = 0;
                int row = index;
                while (col < numberOfColumns) {
                    if (null != rows.get(row)[col] &&
                            row == (int) rows.get(row)[col].getPropertyAsInteger(Property.ROWSPAN) + (int) rows.get(row)[col].getModelElement().getRow() - 1) {
                        CellRenderer cell = rows.get(row)[col];
                        Border cellModelTopBorder = getCellSideBorder(cell.getModelElement(), Property.BORDER_TOP);
                        int colspan = (int) cell.getPropertyAsInteger(Property.COLSPAN);
                        if (null == firstBorderOnCurrentPage.get(col) || (null != cellModelTopBorder && cellModelTopBorder.getWidth() > firstBorderOnCurrentPage.get(col).getWidth())) {
                            for (int i = col; i < col + colspan; i++) {
                                firstBorderOnCurrentPage.set(i, cellModelTopBorder);
                            }
                        }
                        col += colspan;
                        row = index;
                    } else {
                        row++;
                        if (row == rows.size()) {
                            break;
                        }
                    }
                }
            }
            return firstBorderOnCurrentPage;

        } else if (((index == rowRange.getFinishRow() + 1 && !forceNotToProcessAsLast) || index == horizontalBorders.size() - 1)) {
            List<Border> lastBorderOnCurrentPage = getBorderList(bottomBorderCollapseWith, tableBoundingBorders[2], numberOfColumns);
            if (0 != rows.size()) {
                int col = 0;
                int row = index - 1;
                while (col < numberOfColumns) {
                    if (null != rows.get(row)[col]) { // TODO
                        CellRenderer cell = rows.get(row)[col];
                        Border cellModelBottomBorder = getCellSideBorder(cell.getModelElement(), Property.BORDER_BOTTOM);
                        int colspan = (int) cell.getPropertyAsInteger(Property.COLSPAN);
                        if (null == lastBorderOnCurrentPage.get(col) || (null != cellModelBottomBorder && cellModelBottomBorder.getWidth() > lastBorderOnCurrentPage.get(col).getWidth())) {
                            for (int i = col; i < col + colspan; i++) {
                                lastBorderOnCurrentPage.set(i, cellModelBottomBorder);
                            }
                        }
                        col += colspan;
                        row = index - 1;
                    } else {
                        row++;
                        if (row == rows.size()) {
                            break;
                        }
                    }
                }
            }
            return lastBorderOnCurrentPage;
        } else {
            return horizontalBorders.get(index);
        }
    }


    // endregion

    // region setters

    public CollapsedTableBorders setTopBorderCollapseWith(List<Border> topBorderCollapseWith) {
        this.topBorderCollapseWith.clear();
        if (null != topBorderCollapseWith) {
            this.topBorderCollapseWith.addAll(topBorderCollapseWith);
        }
        return this;
    }

    public CollapsedTableBorders setBottomBorderCollapseWith(List<Border> bottomBorderCollapseWith) {
        this.bottomBorderCollapseWith.clear();
        if (null != bottomBorderCollapseWith) {
            this.bottomBorderCollapseWith.addAll(bottomBorderCollapseWith);
        }
        return this;
    }

    //endregion

    // region building border arrays

    protected void prepareBuildingBordersArrays(CellRenderer cell, Border[] tableBorders, int colNum, int row, int col) {
        Border[] cellBorders = cell.getBorders();
        int colspan = (int) cell.getPropertyAsInteger(Property.COLSPAN);
        if (0 == col) {
            cell.setProperty(Property.BORDER_LEFT, getCollapsedBorder(cellBorders[3], tableBorders[3]));
        }
        if (colNum == col + colspan) {
            cell.setProperty(Property.BORDER_RIGHT, getCollapsedBorder(cellBorders[1], tableBorders[1]));
        }
    }

    protected void buildBordersArrays(CellRenderer cell, int row, int col) {
        // We should check if the row number is less than horizontal borders array size. It can happen if the cell with
        // big rowspan doesn't fit current area and is going to be placed partial.
        if (row > horizontalBorders.size()) {
            row--;
        }
        int currCellColspan = (int) cell.getPropertyAsInteger(Property.COLSPAN);

        int nextCellRow;
        int j;

        // consider the cell on the left side of the current one
        if (col != 0 && null == rows.get(row)[col - 1]) {
            j = col;
            do {
                j--;
                nextCellRow = row;
                while (rows.size() != nextCellRow && null == rows.get(nextCellRow)[j]) {
                    nextCellRow++;
                }

            } while (j > 0 && rows.size() != nextCellRow &&
                    (j + rows.get(nextCellRow)[j].getPropertyAsInteger(Property.COLSPAN) != col ||
                            nextCellRow - rows.get(nextCellRow)[j].getPropertyAsInteger(Property.ROWSPAN) + 1 != row));
            if (j >= 0 && nextCellRow != rows.size()) {
                CellRenderer nextCell = rows.get(nextCellRow)[j];
                buildBordersArrays(nextCell, nextCellRow, true);
            }

        }
        // consider cells under the current one
        j = 0;
        while (j < currCellColspan) {
            nextCellRow = row + 1;
            while (nextCellRow < rows.size() && null == rows.get(nextCellRow)[col + j]) {
                nextCellRow++;
            }
            if (nextCellRow == rows.size()) {
                break;
            }
            CellRenderer nextCell = rows.get(nextCellRow)[col + j];
            // otherwise the border was considered previously
            if (row == nextCellRow - nextCell.getPropertyAsInteger(Property.ROWSPAN)) {
                buildBordersArrays(nextCell, nextCellRow, true);
            }
            j += (int) nextCell.getPropertyAsInteger(Property.COLSPAN);
        }

        // consider cells on the right side of the current one
        if (col + currCellColspan < rows.get(row).length) {
            nextCellRow = row;
            while (nextCellRow < rows.size() && null == rows.get(nextCellRow)[col + currCellColspan]) {
                nextCellRow++;
            }
            if (nextCellRow != rows.size()) {
                CellRenderer nextCell = rows.get(nextCellRow)[col + currCellColspan];
                buildBordersArrays(nextCell, nextCellRow, true);
            }
        }
        // consider current cell
        buildBordersArrays(cell, row, false);

    }

    protected void buildBordersArrays(CellRenderer cell, int row, boolean isNeighbourCell) {
        int colspan = (int) cell.getPropertyAsInteger(Property.COLSPAN);
        int rowspan = (int) cell.getPropertyAsInteger(Property.ROWSPAN);
        int colN = cell.getModelElement().getCol();
        Border[] cellBorders = cell.getBorders();

        // cell with big rowspan was splitted
        if (row + 1 - rowspan < 0) {
            rowspan = row + 1;
        }

        // consider top border
        for (int i = 0; i < colspan; i++) {
            checkAndReplaceBorderInArray(horizontalBorders, row + 1 - rowspan, colN + i, cellBorders[0], false);
        }
        // consider bottom border
        for (int i = 0; i < colspan; i++) {
            checkAndReplaceBorderInArray(horizontalBorders, row + 1, colN + i, cellBorders[2], true);
        }
        // process big rowspan
        if (rowspan > 1) {
            int numOfColumns = numberOfColumns;
            for (int k = row - rowspan + 1; k <= row; k++) {
                List<Border> borders = horizontalBorders.get(k);
                if (borders.size() < numOfColumns) {
                    for (int j = borders.size(); j < numOfColumns; j++) {
                        borders.add(null);
                    }
                }
            }
        }
        // consider left border
        for (int j = row - rowspan + 1; j <= row; j++) {
            checkAndReplaceBorderInArray(verticalBorders, colN, j, cellBorders[3], false);
        }
        // consider right border
        for (int i = row - rowspan + 1; i <= row; i++) {
            checkAndReplaceBorderInArray(verticalBorders, colN + colspan, i, cellBorders[1], true);
        }
        // process big colspan
        if (colspan > 1) {
            for (int k = colN; k <= colspan + colN; k++) {
                List<Border> borders = verticalBorders.get(k);
                if (borders.size() < row + rowspan) {
                    for (int l = borders.size(); l < row + rowspan; l++) {
                        borders.add(null);
                    }
                }
            }
        }
    }

    // endregion

    // region lowlevel logic

    protected boolean checkAndReplaceBorderInArray(List<List<Border>> borderArray, int i, int j, Border borderToAdd, boolean hasPriority) {
//        if (borderArray.size() <= i) {
//            for (int count = borderArray.size(); count <= i; count++) {
//                borderArray.add(new ArrayList<Border>());
//            }
//        }
        List<Border> borders = borderArray.get(i);
//        if (borders.isEmpty()) {
//            for (int count = 0; count < j; count++) {
//                borders.add(null);
//            }
//            borders.add(borderToAdd);
//            return true;
//        }
//        if (borders.size() == j) {
//            borders.add(borderToAdd);
//            return true;
//        }
//        if (borders.size() < j) {
//            for (int count = borders.size(); count <= j; count++) {
//                borders.add(count, null);
//            }
//        }
        Border neighbour = borders.get(j);
        if (neighbour == null) {
            borders.set(j, borderToAdd);
            return true;
        } else {
            if (neighbour != borderToAdd) {
                if (borderToAdd != null && neighbour.getWidth() <= borderToAdd.getWidth()) {
                    if (!hasPriority && neighbour.getWidth() == borderToAdd.getWidth()) {
                        return false;
                    }
                    borders.set(j, borderToAdd);
                    return true;
                }
            }
        }

        return false;
    }

    // endregion
}
