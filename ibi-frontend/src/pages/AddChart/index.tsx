import { UploadOutlined } from '@ant-design/icons';
import {Button, Card, Col, Form, Input, message, Row, Select, Space, Spin, Upload} from 'antd';
import React, { useState } from 'react';
import TextArea from 'antd/es/input/TextArea';
import { genChartByAiUsingPost } from '@/services/ibi/chartController';
import ReactECharts from 'echarts-for-react';

const AddChart: React.FC = () => {

  const [chart, setChart] = useState<API.GptResultResponse>();
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [option, setOption] = useState<any>();

  const onFinish = async (values: any) => {
    if (submitting) {
      return;
    }
    setSubmitting(true);
    setOption(undefined);
    setChart(undefined);
    const params = {
      ...values,
      file: undefined,
    };
    try {
      const res = await genChartByAiUsingPost(params, {}, values.file.file.originFileObj);
      console.log(res);
      if (!res?.data) {
        message.error('analysis failed');
      } else {
        message.success('analysis successful');
        const chartOption = JSON.parse(res.data.genChart ?? '');
        if (!chartOption) {
          throw new Error("wrong chart code");
        } else {
          setChart(res.data);
          setOption(chartOption);
        }
      }
    } catch (e: any) {
      message.error('analysis failed' + e.message);
    }
    setSubmitting(false);
  };

  return (
    <div className="add-chart">
      <Row gutter={24}>
        <Col span={12}>
          <Card title={"Intelligent Analysis"}>
            <Form name="addChart" onFinish={onFinish} initialValues={{}}>
              <Form.Item
                name="goal"
                label="Analysis Goal"
                rules={[{ required: true, message: 'Please enter your analysis goal!' }]}
              >
                <TextArea placeholder="Please enter your analysis goal. For example, analyse the growth trend of users" />
              </Form.Item>
              <Form.Item
                name="name"
                label="Chart Name"
                rules={[{ required: true, message: 'Please enter your chart name!' }]}
              >
                <Input placeholder="Please enter your chart name" />
              </Form.Item>
              <Form.Item name="chartType" label="Chart Type">
                <Select
                  placeholder="Please select your chart type"
                  options={[
                    { value: 'Pie Chart', label: 'Pie Chart' },
                    { value: 'Bar Chart', label: 'Bar Chart' },
                    { value: 'Column Chart', label: 'Column Chart' },
                    { value: 'Line Chart', label: 'Line Chart' },
                    { value: 'Scatter Plot', label: 'Scatter Plot' },
                    { value: 'Histogram', label: 'Histogram' },
                    { value: 'Area Chart', label: 'Area Chart' },
                    { value: 'Radar Chart', label: 'Radar Chart' },
                  ]}
                />
              </Form.Item>

              <Form.Item
                name="file"
                label="Analysis Data"
                rules={[{ required: true, message: 'Please upload your analysis data!' }]}
              >
                <Upload name="file" maxCount={1}>
                  <Button icon={<UploadOutlined />}>Click to upload data</Button>
                </Upload>
              </Form.Item>

              <Form.Item wrapperCol={{ span: 12, offset: 6 }}>
                <Space>
                  <Button type="primary" htmlType="submit" loading={submitting} disabled={submitting}>
                    Submit
                  </Button>
                  <Button htmlType="reset">reset</Button>
                </Space>
              </Form.Item>
            </Form>
          </Card>
        </Col>
        <Col span={12}>
          <Card title={"Analysis Result"}>
            <div>
              {chart?.genResult ?? <div>Please submit your analysis request on the left first</div>}
              <Spin spinning={submitting}/>
            </div>
          </Card>
          <Card title={"Generated Chart"}>
            <div>
              {
                option ? <ReactECharts option={option} /> : <div>Please submit your analysis request on the left first</div>
              }
              <Spin spinning={submitting}/>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default AddChart;
