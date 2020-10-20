package io.kontur.eventapi.pdc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

public class HpSrvSearchBody {

    private Order order = new Order();
    private Pagination pagination = new Pagination();
    private List<List<Map<String, String>>> restrictions = new ArrayList<>();

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

    public List<List<Map<String, String>>> getRestrictions() {
        return restrictions;
    }

    /**
     * Add restriction to search criteria <br>
     * Available search types:
     * <ul>
     * <li>LIKE: the field contains value</li>
     * <li>EQUALS: the field and the value are equals</li>
     * <li>NOT_EQUALS: the field and the value are not equals</li>
     * <li>LESS_THAN: the field is lower than the value</li>
     * <li>GREATER_THAN: the field is greater than the value</li>
     * <li>NULL: the field is null</li>
     * <li>NOT_NULL: the field is not null</li>
     * </ul>
     */
    public void addAndRestriction(String searchType, String field, String value) {
        HashMap<String, String> map = new HashMap<>();
        map.put("searchType", searchType);
        map.put(field, value);
        if (restrictions.isEmpty()) {
            restrictions.add(new ArrayList<>());
        }
        restrictions.get(0).add(map);
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

        @Override
        public String toString() {
            return "Pagination{" +
                    "offset=" + offset +
                    ", pageSize=" + pageSize +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pagination that = (Pagination) o;
            return Objects.equals(offset, that.offset) &&
                    Objects.equals(pageSize, that.pageSize);
        }

        @Override
        public int hashCode() {
            return Objects.hash(offset, pageSize);
        }
    }

    public static class Order {
        @JsonProperty("orderlist")
        private Map<String, String> orderList = new LinkedHashMap<>();

        public Map<String, String> getOrderList() {
            return orderList;
        }

        public void setOrderList(Map<String, String> orderList) {
            this.orderList = orderList;
        }

        @Override
        public String toString() {
            return "Order{" +
                    "orderList=" + orderList +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Order order = (Order) o;
            return Objects.equals(orderList, order.orderList);
        }

        @Override
        public int hashCode() {
            return Objects.hash(orderList);
        }
    }

    @Override
    public String toString() {
        return "HpSrvSearchBody{" +
                "order=" + order +
                ", pagination=" + pagination +
                ", restrictions=" + restrictions +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HpSrvSearchBody that = (HpSrvSearchBody) o;
        return Objects.equals(order, that.order) &&
                Objects.equals(pagination, that.pagination) &&
                Objects.equals(restrictions, that.restrictions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(order, pagination, restrictions);
    }
}
