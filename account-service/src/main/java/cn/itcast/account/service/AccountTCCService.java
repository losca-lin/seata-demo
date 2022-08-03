package cn.itcast.account.service;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * @author Losca
 * @date 2022/8/3 11:34
 * localTcc注解声明这是tcc事务
 */
@LocalTCC
public interface AccountTCCService {
    /**
     * TwoPhaseBusinessAction注解下的方法为try逻辑
     * @param userId
     * @param money
     */
    @TwoPhaseBusinessAction(name = "deduct",commitMethod = "confirm",rollbackMethod = "cancel")
    void deduct(@BusinessActionContextParameter(paramName = "userId") String userId,
                @BusinessActionContextParameter(paramName = "money") int money);

    boolean confirm(BusinessActionContext ctx);

    boolean cancel(BusinessActionContext ctx);
}
