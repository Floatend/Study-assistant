package com.example.goalbot.agent.tool;

import com.example.goalbot.agent.ToolCall;
import com.example.goalbot.agent.ToolNames;
import com.example.goalbot.agent.ToolResult;
import org.springframework.stereotype.Component;

@Component
public class HelpTool extends AbstractAgentTool {

    @Override
    public String name() {
        return ToolNames.HELP;
    }

    @Override
    public ToolResult execute(Long userId, ToolCall call) {
        return ToolResult.ok("""
                你可以直接这样说：
                /今日
                /打卡 任务名
                /打卡 任务名 50分钟（实际用时与计划不同时可选填）
                /进度
                /建议
                /复盘
                /周报
                取消今天所有任务
                明天有什么任务

                也可以自然表达：
                今天有什么任务
                周五安排了什么
                明天下午 3 点安排高数复习 60 分钟
                下周的课不上了
                6月22日到6月28日的高数课取消
                打卡物理
                英语听力学了 30 分钟，帮我打卡
                今天有点乱，帮我安排一下
                """);
    }
}
