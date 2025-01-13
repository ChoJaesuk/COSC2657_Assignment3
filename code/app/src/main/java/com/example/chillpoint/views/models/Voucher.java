package com.example.chillpoint.views.models;

public class Voucher {
    private String id;
    private String startDate;
    private String endDate;
    private double amountOfDiscount;
    private String content;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public double getAmountOfDiscount() {
        return amountOfDiscount;
    }

    public void setAmountOfDiscount(double amountOfDiscount) {
        this.amountOfDiscount = amountOfDiscount;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
    @Override
    public String toString() {
        return "Voucher{" +
                "id='" + id + '\'' +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", amountOfDiscount=" + amountOfDiscount +
                ", content='" + content + '\'' +
                '}';
    }

}
