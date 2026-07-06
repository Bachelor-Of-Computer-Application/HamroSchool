package com.hamroschool.util;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

public class PaginationHelper<T> {
    
    private final TableView<T> tableView;
    private final int pageSize;
    private final Label summaryLabel;
    private final Button prevButton;
    private final Button nextButton;
    
    private List<T> allItems = List.of();
    private List<T> filteredItems = List.of();
    private int currentPage = 0;

    /**
     * Create a pagination helper
     * @param tableView The table to paginate
     * @param pageSize Items per page
     * @param summaryLabel Label to show "Showing X of Y" text
     * @param prevButton Previous page button
     * @param nextButton Next page button
     */
    public PaginationHelper(TableView<T> tableView, int pageSize, Label summaryLabel, 
                            Button prevButton, Button nextButton) {
        this.tableView = tableView;
        this.pageSize = pageSize;
        this.summaryLabel = summaryLabel;
        this.prevButton = prevButton;
        this.nextButton = nextButton;
    }

    public void setAllItems(List<T> items) {
        this.allItems = items;
        this.filteredItems = items;
        this.currentPage = 0;
        renderCurrentPage();
    }

    public void setFilteredItems(List<T> items) {
        this.filteredItems = items;
        this.currentPage = 0;
        renderCurrentPage();
    }


    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            renderCurrentPage();
        }
    }

    public void nextPage() {
        int totalPages = getTotalPages();
        if (currentPage < totalPages - 1) {
            currentPage++;
            renderCurrentPage();
        }
    }

    public int getCurrentPage() {
        return currentPage;
    }


    public int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) filteredItems.size() / pageSize));
    }

    public List<T> getAllItems() {
        return allItems;
    }

    public List<T> getFilteredItems() {
        return filteredItems;
    }

    private void renderCurrentPage() {
        if (filteredItems.isEmpty()) {
            tableView.setItems(FXCollections.emptyObservableList());
            updateSummary(0, 0, 0);
            updateButtons(1);
            return;
        }

        int totalPages = getTotalPages();
        if (currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }

        int from = currentPage * pageSize;
        int to = Math.min(from + pageSize, filteredItems.size());

        ObservableList<T> pageItems = FXCollections.observableArrayList(
            filteredItems.subList(from, to)
        );
        
        tableView.setItems(pageItems);
        updateSummary(from + 1, to, filteredItems.size());
        updateButtons(totalPages);
    }

    private void updateSummary(int from, int to, int total) {
        if (summaryLabel != null) {
            if (total == 0) {
                summaryLabel.setText("Showing 0 of 0");
            } else {
                summaryLabel.setText("Showing " + from + "–" + to + " of " + total);
            }
        }
    }

    private void updateButtons(int totalPages) {
        if (prevButton != null) {
            prevButton.setDisable(currentPage == 0);
        }
        if (nextButton != null) {
            nextButton.setDisable(currentPage >= totalPages - 1);
        }
    }

    public void refresh() {
        renderCurrentPage();
    }

    public void updateSummaryWithItemName(String itemName) {
        if (summaryLabel != null) {
            int total = filteredItems.size();
            if (total == 0) {
                summaryLabel.setText("No " + itemName + " found");
            } else {
                int from = currentPage * pageSize + 1;
                int to = Math.min((currentPage + 1) * pageSize, total);
                summaryLabel.setText("Showing " + from + "–" + to + " of " + total + " " + itemName);
            }
        }
    }
}
