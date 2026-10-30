package com.campusfix.dto;

import com.campusfix.model.Category;
import com.campusfix.model.Complaint;
import com.campusfix.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ComplaintForm {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must be 150 characters or fewer")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be 10 to 2000 characters")
    private String description;

    @NotNull(message = "Select a category")
    private Category category;

    @NotNull(message = "Select a priority")
    private Priority priority = Priority.MEDIUM;

    @NotBlank(message = "Location is required")
    @Size(max = 150, message = "Location must be 150 characters or fewer")
    private String location;

    public static ComplaintForm from(Complaint c) {
        ComplaintForm f = new ComplaintForm();
        f.title = c.getTitle();
        f.description = c.getDescription();
        f.category = c.getCategory();
        f.priority = c.getPriority();
        f.location = c.getLocation();
        return f;
    }
}
