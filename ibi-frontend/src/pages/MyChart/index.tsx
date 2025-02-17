import React, {useEffect, useState} from 'react';
import {listChartByPageUsingPost} from "@/services/ibi/chartController";
import {Avatar, Card, List, message} from 'antd';
import ReactECharts from "echarts-for-react";
import Search from "antd/es/input/Search";

const MyChartPage: React.FC = () => {

  const initSearchParams = {
    current: 1,
    pageSize: 4,
  }

  const [searchParams, setSearchParams] = useState<API.ChartQueryRequest>({...initSearchParams});
  const [chartList, setChartList] = useState<API.Chart[]>();
  const [total, setTotal] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(true);

  const loadData = async () => {
    setLoading(true);
    try {
      const res = await listChartByPageUsingPost(searchParams);
      if (res.data) {
        setChartList(res.data.records ?? []);
        setTotal(res.data.total ?? 0);

        if (res.data.records) {
          res.data.records.forEach(data => {
            const chartOption = JSON.parse(data.genChart ?? '{}');
            chartOption.title = undefined;
            data.genChart = JSON.stringify(chartOption);
          })
        }
      } else {
        message.error('error: failed to get chart list, no data');
      }
    } catch (e: any) {
      message.error('error: failed to get chart list' + e.message);
    }
    setLoading(false);
  }

  useEffect(() => {
    loadData();
  }, [searchParams]);

  return (
    <div className="my-chart-page">
      <div>
        <Search placeholder={"Input chart name to search"} enterButton loading={loading} onSearch={(value) => {
          setSearchParams({
            ...initSearchParams,
            name: value,
          })
        }} />
      </div>
      <div className={"margin-16"} />
      <List
        grid = {{
          gutter: 16,
          xs: 1,
          sm: 1,
          md: 1,
          lg: 2,
          xl: 2,
          xxl: 2,
        }}
        pagination={{
          onChange: (page, pageSize) => {
            setSearchParams({
              ...searchParams,
              current: page,
              pageSize: pageSize,
            })
          },
          current: searchParams.current,
          pageSize: searchParams.pageSize,
          total: total
        }}
        loading={loading}
        dataSource={chartList}
        // footer={
        //   <div>
        //     <b>ant design</b> footer part
        //   </div>
        // }
        renderItem={(item) => (
          <List.Item
            key={item.id}
          >
            <Card style={{ width: '100%' }}>
            <List.Item.Meta
              avatar={<Avatar src={'https://api.dicebear.com/7.x/miniavs/svg?seed=1'} />}
              title={item.name}
              description={item.chartType ? ('Chart Type: ' + item.chartType) : undefined }
            />
            <div style={{ marginBottom: 16}} />
            {'Analysis Goal: ' + item.goal}
            <div style={{ marginBottom: 16}} />
            <Card>
              <ReactECharts option={JSON.parse(item.genChart ?? '{}')} />
            </Card>
            </Card>
          </List.Item>
        )}
      />
    </div>
  );
};

export default MyChartPage;
