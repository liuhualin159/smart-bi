package dev.lhl.system.service;

import dev.lhl.common.core.domain.DataPermission;
import java.util.List;

/**
 * 数据权限Service接口
 * 
 * @author smart-bi
 */
public interface IDataPermissionService
{
    /**
     * 查询数据权限
     * 
     * @param id 权限ID
     * @return 数据权限
     */
    public DataPermission selectDataPermissionById(Long id);

    /**
     * 查询数据权限列表
     * 
     * @param dataPermission 数据权限
     * @return 数据权限集合
     */
    public List<DataPermission> selectDataPermissionList(DataPermission dataPermission);

    /**
     * 根据用户ID和表名查询权限
     * 
     * @param userId 用户ID
     * @param tableName 表名
     * @return 数据权限集合
     */
    public List<DataPermission> selectDataPermissionByUserAndTable(Long userId, String tableName);

    /**
     * 根据角色ID和表名查询权限
     * 
     * @param roleId 角色ID
     * @param tableName 表名
     * @return 数据权限集合
     */
    public List<DataPermission> selectDataPermissionByRoleAndTable(Long roleId, String tableName);

    /**
     * 检查用户是否有表访问权限
     * 
     * @param userId 用户ID
     * @param tableName 表名
     * @return true-有权限，false-无权限
     */
    public boolean checkTablePermission(Long userId, String tableName);

    /**
     * 检查用户是否有字段访问权限
     * 
     * @param userId 用户ID
     * @param tableName 表名
     * @param fieldName 字段名
     * @return true-有权限，false-无权限
     */
    public boolean checkFieldPermission(Long userId, String tableName, String fieldName);

    /**
     * 获取用户对表的行级过滤条件
     * 
     * @param userId 用户ID
     * @param tableName 表名
     * @return 行级过滤条件（SQL WHERE子句）
     */
    public String getRowFilter(Long userId, String tableName);

    /**
     * 新增数据权限
     * 
     * @param dataPermission 数据权限
     * @return 结果
     */
    public int insertDataPermission(DataPermission dataPermission);

    /**
     * 修改数据权限
     * 
     * @param dataPermission 数据权限
     * @return 结果
     */
    public int updateDataPermission(DataPermission dataPermission);

    /**
     * 批量删除数据权限
     * 
     * @param ids 需要删除的权限ID
     * @return 结果
     */
    public int deleteDataPermissionByIds(Long[] ids);

    /**
     * 删除数据权限信息
     * 
     * @param id 权限ID
     * @return 结果
     */
    public int deleteDataPermissionById(Long id);
}
