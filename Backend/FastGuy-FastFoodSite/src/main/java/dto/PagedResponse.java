package dto;

import java.util.List;

public class PagedResponse<T> {
    private List<T> items;
    private int total;
    private int page;
    private int limit;

    public PagedResponse() {}

    public PagedResponse(List<T> items, int total, int page, int limit) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.limit = limit;
    }

    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
