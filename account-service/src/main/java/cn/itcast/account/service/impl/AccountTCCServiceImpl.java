package cn.itcast.account.service.impl;

import cn.itcast.account.entity.AccountFreeze;
import cn.itcast.account.mapper.AccountFreezeMapper;
import cn.itcast.account.mapper.AccountMapper;
import cn.itcast.account.service.AccountTCCService;
import io.seata.core.context.RootContext;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static cn.itcast.account.entity.AccountFreeze.State.CANCEL;

/**
 * @author Losca
 * @date 2022/8/3 11:42
 */
@Service
public class AccountTCCServiceImpl implements AccountTCCService {
    @Autowired
    private AccountMapper accountMapper;
    @Autowired
    private AccountFreezeMapper accountFreezeMapper;

    /**
     * try业务逻辑
     * @param userId
     * @param money
     */
    @Override
    @Transactional
    public void deduct(String userId, int money) {
        //获取事务id
        String xid = RootContext.getXID();
        //避免业务悬挂
        AccountFreeze id = accountFreezeMapper.selectById(xid);
        //id为空说明没有进行过try操作，不为空则进行过cancel操作，拒绝执行
        if (id != null) {
            return;
        }
        //扣减可用余额
        accountMapper.deduct(userId, money);
        //记录冻结金额，事务状态
        AccountFreeze accountFreeze = new AccountFreeze();
        accountFreeze.setUserId(userId);
        accountFreeze.setFreezeMoney(money);
        accountFreeze.setState(AccountFreeze.State.TRY);
        accountFreeze.setXid(xid);
        accountFreezeMapper.insert(accountFreeze);
    }

    @Override
    public boolean confirm(BusinessActionContext ctx) {
        //获取事务id
        String xid = ctx.getXid();
        //根据id删除冻结记录
        int count = accountFreezeMapper.deleteById(xid);
        return count == 1;
    }

    @Override
    @Transactional
    public boolean cancel(BusinessActionContext ctx) {
        //查询冻结记录
        String xid = ctx.getXid();
        AccountFreeze accountFreeze = accountFreezeMapper.selectById(xid);
        String userId = (String) ctx.getActionContext("userId");
        //避免空回滚 accountFreeze为null说明try为执行，为空回滚
        if (accountFreeze == null) {
            accountFreeze = new AccountFreeze();
            accountFreeze.setXid(xid);
            accountFreeze.setFreezeMoney(0);
            accountFreeze.setUserId(userId);
            accountFreeze.setState(CANCEL);
            accountFreezeMapper.insert(accountFreeze);
            return true;
        }
        //幂等判断 accountFreeze状态是cancel说明已经做过业务了，直接返回
        if (accountFreeze.getState() == CANCEL){
            return true;
        }
        //恢复可用余额
        accountMapper.refund(accountFreeze.getUserId(), accountFreeze.getFreezeMoney());
        //将冻结金额清零，状态改为cancel
        accountFreeze.setFreezeMoney(0);
        accountFreeze.setState(CANCEL);
        int count = accountFreezeMapper.updateById(accountFreeze);
        return count == 1;
    }
}
