package dev.lhl.dashboard.mapper;

import dev.lhl.dashboard.domain.ShareLink;
import java.util.List;

/**
 * 分享链接Mapper接口
 * 
 * @author smart-bi
 */
public interface ShareLinkMapper
{
    ShareLink selectShareLinkById(Long id);
    ShareLink selectShareLinkByShareKey(String shareKey);
    List<ShareLink> selectShareLinkList(ShareLink shareLink);
    int insertShareLink(ShareLink shareLink);
    int updateShareLink(ShareLink shareLink);
    int deleteShareLinkById(Long id);
    int deleteShareLinkByIds(Long[] ids);
    int incrementAccessCount(String shareKey);
}
