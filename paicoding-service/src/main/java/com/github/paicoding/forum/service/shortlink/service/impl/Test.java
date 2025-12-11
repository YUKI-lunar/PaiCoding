package com.github.paicoding.forum.service.shortlink.service.impl;

import com.github.paicoding.forum.api.model.context.ReqInfoContext;
import com.github.paicoding.forum.api.model.enums.YesOrNoEnum;
import com.github.paicoding.forum.api.model.vo.shortlink.ShortLinkVO;
import com.github.paicoding.forum.api.model.vo.shortlink.dto.ShortLinkDTO;
import com.github.paicoding.forum.core.cache.RedisClient;
import com.github.paicoding.forum.service.shortlink.help.ShortCodeGenerator;
import com.github.paicoding.forum.service.shortlink.help.SourceDetector;
import com.github.paicoding.forum.service.shortlink.repository.entity.ShortLinkDO;
import com.github.paicoding.forum.service.shortlink.repository.entity.ShortLinkRecordDO;
import com.github.paicoding.forum.service.shortlink.repository.mapper.ShortLinkMapper;
import com.github.paicoding.forum.service.shortlink.repository.mapper.ShortLinkRecordMapper;
import com.github.paicoding.forum.service.shortlink.service.ShortLinkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class Test implements ShortLinkService {

    private static final String REDIS_SHORT_LINK_PREFIX = "short_link:";

    @Resource
    private ShortLinkMapper shortLinkMapper;

    @Resource
    private ShortLinkRecordMapper shortLinkRecordMapper;

    @Value("${view.site.host:https://paicoding.com}")
    private String host;

    public Test(ShortLinkMapper shortLinkMapper, ShortLinkRecordMapper shortLinkRecordMapper) {
        this.shortLinkMapper = shortLinkMapper;
        this.shortLinkRecordMapper = shortLinkRecordMapper;
    }

    @Value("#{'${short-link.whitelist:}'.split(',')}")
    private List<String> domainWhitelist;


    @Override
    public ShortLinkVO createShortLink(ShortLinkDTO shortLinkDTO) throws NoSuchAlgorithmException {
        if(log.isDebugEnabled()) {
            log.debug("Creating short link for URL: {}", shortLinkDTO.getOriginalUrl());
        }
        //检查一下在不在白名单
        if (!isUrlInWhitelist(shortLinkDTO.getOriginalUrl())) {
            log.warn(shortLinkDTO.getOriginalUrl() + " is not in whitelist");
            throw new RuntimeException("不允许为此域名创建短链接");
        }
        //拿到path,也就是除了协议和域名,剩下的
        String path = shortLinkDTO.getOriginalUrl().replaceAll("^(https?://|http://[^/]+)(/.*)?$", "$2");

        //生成短码
        String shortCode = generateUniqueShortCode(path);

        //构建do对象
        ShortLinkDO shortLinkDO = createShortLinkDO(shortLinkDTO, shortCode);

        //保存映射到db
        int shortLinkId = shortLinkMapper.getIdAfterInsert(shortLinkDO);
        if (log.isDebugEnabled()) {
            log.debug("short id created with id: {}", shortLinkId);
        }
        //把映射上传到cache
        RedisClient.hSet(REDIS_SHORT_LINK_PREFIX,shortCode,shortLinkDO.getOriginalUrl());

        //保存记录到db
        ShortLinkRecordDO shortLinkRecordDO = createShortLinkRecordDO(shortLinkDO.getShortCode(), shortLinkDTO);
        shortLinkRecordMapper.insert(shortLinkRecordDO);

        if (log.isDebugEnabled()) {
            log.debug("Short link record saved for short code: {}", shortCode);
        }
        return createShortLinkVO(shortLinkDO);
        }

    @Override
    public ShortLinkVO getOriginalLink(String shortcode){
        if (log.isDebugEnabled()) {
            log.debug("Fetching original link for short code: {}", shortcode);
        }

        String originalUrl = getOriginalUrlFromCacheOrDb(shortcode);

        if(!StringUtils.hasText(originalUrl)) {
            log.error("Short link not found for short code: {}", shortcode);
            throw new RuntimeException("short link not found");
        }
        //因为要存记录,所以看一下当前进来的id
        String paramUserId = (ReqInfoContext.getReqInfo().getUserId()==null) ?
                "0" : ReqInfoContext.getReqInfo().getUserId().toString();
        log.info("short link,shortcode:{},url:{},userId:{}",shortcode,originalUrl,paramUserId);
        return new ShortLinkVO(originalUrl, originalUrl);
    }

    private ShortLinkVO createShortLinkVO(ShortLinkDO shortLinkDO){
        ShortLinkVO shortLinkVO = new ShortLinkVO();
        shortLinkVO.setOriginalUrl(shortLinkDO.getOriginalUrl());
        //短链接统一在"/sol/"下标识,前端好解算
        shortLinkVO.setShortUrl(host + "/sol/" + shortLinkDO.getShortCode());
        return shortLinkVO;
    }

    private String generateUniqueShortCode(String path)throws  NoSuchAlgorithmException {
        //允许尝试三次,生成不了就算了
        String shortCode;
        int i =0;
        do{
            shortCode = generateUniqueShortCode(path);
            i++;
        }while(shortLinkMapper.getByShortCode(shortCode) != null && i<3);
        return shortCode;
    }

    private ShortLinkDO createShortLinkDO(ShortLinkDTO shortLinkDTO, String shortCode) {
        long currentTimeMillis = System.currentTimeMillis();
        Date currentDate = new Date(currentTimeMillis);
        ShortLinkDO shortLinkDO = new ShortLinkDO();
        shortLinkDO.setShortCode(shortCode);
        shortLinkDO.setCreateTime(currentDate);
        shortLinkDO.setUpdateTime(currentDate);
        shortLinkDO.setDeleted(YesOrNoEnum.NO.getCode());
        shortLinkDO.setOriginalUrl(shortLinkDTO.getOriginalUrl());
        return shortLinkDO;
    }

    private ShortLinkRecordDO createShortLinkRecordDO(String shortCode,ShortLinkDTO shortLinkDTO){
        ShortLinkRecordDO shortLinkRecordDO = new ShortLinkRecordDO();
        shortLinkRecordDO.setShortCode(shortCode);
        shortLinkRecordDO.setUserId(shortLinkDTO.getUserId());
        shortLinkRecordDO.setAccessTime(System.currentTimeMillis());
        shortLinkRecordDO.setLoginMethod("Unknown");
        shortLinkRecordDO.setIpAddress(ReqInfoContext.getReqInfo().getClientIp());
        shortLinkRecordDO.setAccessSource(SourceDetector.detectSource());
        return shortLinkRecordDO;

    }
    private String getOriginalUrlFromCacheOrDb(String shortCode) {

    }







    }
