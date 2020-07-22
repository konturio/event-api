package io.kontur.eventapi.pdc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class HpSrvSearchBody {

    private Order order = new Order();
    private Pagination pagination = new Pagination();

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public static class Pagination {

        private Integer offset;

        @JsonProperty("pagesize")
        private Integer pageSize;

        public Integer getOffset() {
            return offset;
        }

        public void setOffset(Integer offset) {
            this.offset = offset;
        }

        public Integer getPageSize() {
            return pageSize;
        }

        public void setPageSize(Integer pageSize) {
            this.pageSize = pageSize;
        }
    }

    public static class Order {
        @JsonProperty("orderlist")
        private Map<String, String> orderList = new HashMap<>();

        public Map<String, String> getOrderList() {
            return orderList;
        }

        public void setOrderList(Map<String, String> orderList) {
            this.orderList = orderList;
        }
    }
}
