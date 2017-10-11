package com.Pojo;

import org.springframework.stereotype.Component;

/**
 * Created by Administrator on 2017/10/10.
 */

@Component
public class Permission {
    private int id;
    private String token;
    /**资源url**/
    private String url;
    /**权限说明**/
    private String description;
    /**所属角色编号**/
    private int roleId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    @Override
    public String toString() {
        return "PermissionPojo{" +
                "id=" + id +
                ", token='" + token + '\'' +
                ", url='" + url + '\'' +
                ", description='" + description + '\'' +
                ", roleId=" + roleId +
                '}';
    }
}
