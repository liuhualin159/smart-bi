package dev.lhl.system.service.impl;

import dev.lhl.common.core.domain.DataPermission;
import dev.lhl.common.core.domain.entity.SysUser;
import dev.lhl.common.utils.SecurityUtils;
import dev.lhl.common.utils.StringUtils;
import dev.lhl.system.mapper.DataPermissionMapper;
import dev.lhl.system.service.IDataPermissionService;
import dev.lhl.system.service.ISysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据权限Service业务层处理
 * 
 * @author smart-bi
 */
@Service
public class DataPermissionServiceImpl implements IDataPermissionService
{
    private static final Logger log = LoggerFactory.getLogger(DataPermissionServiceImpl.class);

    @Autowired
    private DataPermissionMapper dataPermissionMapper;
    
    @Autowired
    private ISysUserService userService;

    @Override
    public DataPermission selectDataPermissionById(Long id)
    {
        return dataPermissionMapper.selectDataPermissionById(id);
    }

    @Override
    public List<DataPermission> selectDataPermissionList(DataPermission dataPermission)
    {
        return dataPermissionMapper.selectDataPermissionList(dataPermission);
    }

    @Override
    public List<DataPermission> selectDataPermissionByUserAndTable(Long userId, String tableName)
    {
        return dataPermissionMapper.selectDataPermissionByUserAndTable(userId, tableName);
    }

    @Override
    public List<DataPermission> selectDataPermissionByRoleAndTable(Long roleId, String tableName)
    {
        return dataPermissionMapper.selectDataPermissionByRoleAndTable(roleId, tableName);
    }

    @Override
    public boolean checkTablePermission(Long userId, String tableName)
    {
        if (userId == null || StringUtils.isEmpty(tableName))
        {
            return false;
        }

        try
        {
            // 获取用户的所有角色ID
            SysUser user = userService.selectUserById(userId);
            if (user == null || user.isAdmin())
            {
                // 管理员默认有所有权限
                return true;
            }

            List<Long> roleIds = user.getRoles().stream()
                .map(role -> role.getRoleId())
                .collect(Collectors.toList());

            // 检查用户级权限
            List<DataPermission> userPermissions = dataPermissionMapper.selectDataPermissionByUserAndTable(userId, tableName);
            for (DataPermission perm : userPermissions)
            {
                if ("TABLE".equals(perm.getPermissionType()) && "DENY".equals(perm.getOperation()))
                {
                    // 有DENY权限，直接拒绝
                    return false;
                }
                if ("TABLE".equals(perm.getPermissionType()) && "ALLOW".equals(perm.getOperation()))
                {
                    // 有ALLOW权限，允许访问
                    return true;
                }
            }

            // 检查角色级权限
            for (Long roleId : roleIds)
            {
                List<DataPermission> rolePermissions = dataPermissionMapper.selectDataPermissionByRoleAndTable(roleId, tableName);
                for (DataPermission perm : rolePermissions)
                {
                    if ("TABLE".equals(perm.getPermissionType()) && "DENY".equals(perm.getOperation()))
                    {
                        // 有DENY权限，直接拒绝
                        return false;
                    }
                    if ("TABLE".equals(perm.getPermissionType()) && "ALLOW".equals(perm.getOperation()))
                    {
                        // 有ALLOW权限，允许访问
                        return true;
                    }
                }
            }

            // 默认：如果没有明确配置，则允许访问（可以根据业务需求调整）
            return true;
        }
        catch (Exception e)
        {
            log.error("检查表权限失败: userId={}, tableName={}", userId, tableName, e);
            // 出错时默认拒绝访问，保证安全
            return false;
        }
    }

    @Override
    public boolean checkFieldPermission(Long userId, String tableName, String fieldName)
    {
        if (userId == null || StringUtils.isEmpty(tableName) || StringUtils.isEmpty(fieldName))
        {
            return false;
        }

        // 先检查表级权限
        if (!checkTablePermission(userId, tableName))
        {
            return false;
        }

        try
        {
            // 获取用户的所有角色ID
            SysUser user = userService.selectUserById(userId);
            if (user == null || user.isAdmin())
            {
                // 管理员默认有所有权限
                return true;
            }

            List<Long> roleIds = user.getRoles().stream()
                .map(role -> role.getRoleId())
                .collect(Collectors.toList());

            // 检查用户级字段权限
            List<DataPermission> userPermissions = dataPermissionMapper.selectDataPermissionByUserAndTable(userId, tableName);
            for (DataPermission perm : userPermissions)
            {
                if ("FIELD".equals(perm.getPermissionType()) && fieldName.equals(perm.getFieldName()))
                {
                    if ("DENY".equals(perm.getOperation()))
                    {
                        return false;
                    }
                    if ("ALLOW".equals(perm.getOperation()))
                    {
                        return true;
                    }
                }
            }

            // 检查角色级字段权限
            for (Long roleId : roleIds)
            {
                List<DataPermission> rolePermissions = dataPermissionMapper.selectDataPermissionByRoleAndTable(roleId, tableName);
                for (DataPermission perm : rolePermissions)
                {
                    if ("FIELD".equals(perm.getPermissionType()) && fieldName.equals(perm.getFieldName()))
                    {
                        if ("DENY".equals(perm.getOperation()))
                        {
                            return false;
                        }
                        if ("ALLOW".equals(perm.getOperation()))
                        {
                            return true;
                        }
                    }
                }
            }

            // 默认：如果没有明确配置字段权限，则允许访问
            return true;
        }
        catch (Exception e)
        {
            log.error("检查字段权限失败: userId={}, tableName={}, fieldName={}", userId, tableName, fieldName, e);
            // 出错时默认拒绝访问，保证安全
            return false;
        }
    }

    @Override
    public String getRowFilter(Long userId, String tableName)
    {
        if (userId == null || StringUtils.isEmpty(tableName))
        {
            return null;
        }

        try
        {
            // 获取用户的所有角色ID
            SysUser user = userService.selectUserById(userId);
            if (user == null || user.isAdmin())
            {
                // 管理员不需要行级过滤
                return null;
            }

            List<Long> roleIds = user.getRoles().stream()
                .map(role -> role.getRoleId())
                .collect(Collectors.toList());

            // 检查用户级行级权限
            List<DataPermission> userPermissions = dataPermissionMapper.selectDataPermissionByUserAndTable(userId, tableName);
            for (DataPermission perm : userPermissions)
            {
                if ("ROW".equals(perm.getPermissionType()) && StringUtils.isNotEmpty(perm.getRowFilter()))
                {
                    return perm.getRowFilter();
                }
            }

            // 检查角色级行级权限
            for (Long roleId : roleIds)
            {
                List<DataPermission> rolePermissions = dataPermissionMapper.selectDataPermissionByRoleAndTable(roleId, tableName);
                for (DataPermission perm : rolePermissions)
                {
                    if ("ROW".equals(perm.getPermissionType()) && StringUtils.isNotEmpty(perm.getRowFilter()))
                    {
                        return perm.getRowFilter();
                    }
                }
            }

            return null;
        }
        catch (Exception e)
        {
            log.error("获取行级过滤条件失败: userId={}, tableName={}", userId, tableName, e);
            return null;
        }
    }

    @Override
    public int insertDataPermission(DataPermission dataPermission)
    {
        dataPermission.setCreateBy(SecurityUtils.getUsername());
        return dataPermissionMapper.insertDataPermission(dataPermission);
    }

    @Override
    public int updateDataPermission(DataPermission dataPermission)
    {
        dataPermission.setUpdateBy(SecurityUtils.getUsername());
        return dataPermissionMapper.updateDataPermission(dataPermission);
    }

    @Override
    public int deleteDataPermissionByIds(Long[] ids)
    {
        return dataPermissionMapper.deleteDataPermissionByIds(ids);
    }

    @Override
    public int deleteDataPermissionById(Long id)
    {
        return dataPermissionMapper.deleteDataPermissionById(id);
    }
}
