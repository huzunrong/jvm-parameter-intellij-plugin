package io.github.newhoo.jvm.setting;

import com.intellij.openapi.project.Project;
import com.intellij.ui.TableUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import io.github.newhoo.jvm.util.AppUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * SettingForm
 *
 * @author huzunrong
 * @since 1.0
 */
public class SettingForm {
    public JPanel mainPanel;

    private JPanel decorationLayoutPanel;
    public JTextField jvmParameterText;
    private JPanel jvmParameterGeneratePanel;

    private final Project project;

    private MyJvmTableModel dataModel = new MyJvmTableModel();
    private TableModelListener tableModelListener = e -> {
        String jvmParameter = dataModel.list.stream()
                                            .filter(cs -> Objects.equals(true, cs[0]))
                                            .filter(cs -> StringUtils.isNotEmpty(String.valueOf(cs[1])))
                                            .map(cs -> {
                                                String c2 = String.valueOf(cs[2]);
                                                if (StringUtils.isEmpty(c2)) {
                                                    return String.valueOf(cs[1]);
                                                }
                                                return !StringUtils.containsWhitespace(c2)
                                                        ? "-D" + cs[1] + "=" + c2
                                                        : "\"-D" + cs[1] + "=" + c2 + "\"";
                                            })
                                            .collect(Collectors.joining(" "));
        jvmParameterText.setText(jvmParameter);
        jvmParameterText.setToolTipText(jvmParameter);
    };

    public SettingForm(Project project) {
        this.project = project;

        init();
    }

    private void init() {
        dataModel.addTableModelListener(tableModelListener);

        JBTable jbTable = new JBTable(dataModel);
        jbTable.getColumnModel().getColumn(0).setMaxWidth(40);
        ToolbarDecorator decorationToolbar = ToolbarDecorator.createDecorator(jbTable);

        decorationToolbar.setAddAction(button -> {
            EventQueue.invokeLater(dataModel::addRow);
        });
        decorationToolbar.setRemoveAction(button -> {
            EventQueue.invokeLater(() -> {
                TableUtil.removeSelectedItems(jbTable);
            });
        });
        decorationLayoutPanel.add(decorationToolbar.createPanel(), BorderLayout.CENTER);

        // 生成快捷按钮
        generateButton();
    }

    private void generateButton() {
        JButton jvmMemoryBtn = new JButton("jvm内存设置");
        jvmMemoryBtn.addActionListener(l -> {
            dataModel.addRow(true, "-Xms512m -Xmx512m", "");
        });
        JButton apolloBtn = new JButton("Apollo启动环境");
        apolloBtn.addActionListener(l -> {
            dataModel.addRow(true, "env", "DEV");
            dataModel.addRow(false, "idc", "default");
        });
        JButton dubboLocalDevBtn = new JButton("dubbo本地开发");
        dubboLocalDevBtn.addActionListener(l -> {
            dataModel.addRow(true, "dubbo.registry.register", "false");
            dataModel.addRow(true, "dubbo.service.group", System.getProperty("user.name"));
        });
        JButton dubboGroupBtn = new JButton("dubbo服务分组");
        dubboGroupBtn.addActionListener(l -> {
            AppUtils.findDubboService(this.project).forEach(s -> {
                dataModel.addRow(true, "dubbo.service." + s + ".group", System.getProperty("user.name"));
            });
        });
        jvmParameterGeneratePanel.add(jvmMemoryBtn);
        jvmParameterGeneratePanel.add(apolloBtn);
        jvmParameterGeneratePanel.add(dubboLocalDevBtn);
        jvmParameterGeneratePanel.add(dubboGroupBtn);
    }

    public String getJvmParameterTableText() {
        return dataModel.list.stream()
                             .flatMap(objects -> Stream.of(objects)
                                                       .map(o -> StringUtils.isEmpty(o.toString()) ? " " : o.toString()))
                             .collect(Collectors.joining("@@@"));
    }

    public void setJvmParameterTableText(String jvmParameterTableText) {
        String[] split = StringUtils.split(jvmParameterTableText, "@@@");
        if (split == null || split.length % 3 != 0) {
            return;
        }

        dataModel.clear();
        for (int i = 0; i < split.length; i = i + 3) {
            dataModel.addRow(BooleanUtils.toBoolean(split[i]), StringUtils.trimToEmpty(split[i + 1]),
                    StringUtils.trimToEmpty(split[i + 2]));
        }
    }
}