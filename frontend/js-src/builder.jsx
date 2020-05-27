'use strict';

const e = React.createElement;

class Counter {
    constructor() {
        this.id = 0;
    }

    nextId() {
        let current = this.id;
        this.id = this.id + 1;
        return current;
    }
}

class Content extends React.Component {
    constructor(props) {
        super(props);
        this.walk = this.walk.bind(this);
        this.shuffle = this.shuffle.bind(this);
        this.handleUp = this.handleUp.bind(this);
        this.handleDown = this.handleDown.bind(this);
        this.dispatch = this.dispatch.bind(this);
        this.handleAdd = this.handleAdd.bind(this);
        this.handleDel = this.handleDel.bind(this);
        this.handleButton = this.handleButton.bind(this);
        this.multiChange = this.multiChange.bind(this);
        this.counter = new Counter();

        let addId = (obj) => {
            let toReturn = Object.assign({},obj);
            toReturn.id = this.counter.nextId();
            if(obj.type == 'playlist') {
                console.log("Giving id " + toReturn.id + " to " + obj.name);
                return toReturn;
            } else if (obj.type == 'multi') {
                toReturn.playlist = addId(toReturn.playlist);
                return toReturn;
            } else {
                toReturn.playlists = toReturn.playlists.map(addId);
                return toReturn;
            }
        };

        this.state = {
            schedule: addId(props.schedule),
            history: [],
            mode: props.mode,
            visible:  {table:"none",
                       builder:"block",
                       json:"none"}
        };


    }



    handleButton(id) {
        let update = {table:"none",
                      builder:"none",
                      json:"none"};
        update[id] = "block";
        this.setState({visible:update});
    }

    render() {
        return (
            <div>
              <h2>Schedule: {this.state.schedule.name}</h2>
              <ClickTab keyName="table"
                        selected={this.state.visible["table"] == "block"}
                        handleButton={this.handleButton} />
              <ClickTab keyName="builder"
                        selected={this.state.visible["builder"] == "block"}
                        handleButton={this.handleButton} />
              <ClickTab keyName="json"
                        selected={this.state.visible["json"] == "block"}
                        handleButton={this.handleButton}/>
              <div style={{display: this.state.visible.table}}>
                <MakeTable playlist={this.state.schedule.playlists} />
              </div>
              <div style={{display: this.state.visible.builder}}>
                <Schedule
                  playlists={this.state.schedule.playlists}
                  handlers={{
                      handleUp: this.handleUp,
                      handleDown: this.handleDown,
                      handleAdd: this.handleAdd,
                      handleDel: this.handleDel,
                      multiChange: this.multiChange,
                      addId: this.addId
                  }} />
              </div>
              <div style={{display: this.state.visible.json}}>
                <form method="post" action="schedule-builder.html">
                  <textarea className={this.state.schedule.name}
                            name="schedule-body"
                            readOnly={true}
                            value={JSON.stringify(this.state.schedule,null,2)}
                  />
                  <input type="hidden"
                         readOnly={true}
                         name="mode"
                         value={this.state.mode} />
                  <input type="hidden"
                         readOnly={true}
                         name="schedule-name"
                         value={this.state.schedule.name} />
                  <input type="submit" value="Submit" />
                </form>
              </div>
            </div>
        );
    }

    dispatch(obj) {
        return {
            'playlist' : obj,
            'merge' : obj.playlists,
            'multi' : [obj.playlist],
            'complex' : obj.playlists
        }[obj.type];
    }

    walk(index) {
        let playlist = this.state.schedule.playlists;
        let idx = index.slice();
        while(idx.length > 1) {
            playlist = Array.isArray(playlist) ?
                this.dispatch(playlist[idx[0]]) :
                this.dispatch(playlist);
            idx = idx.slice(1,);
        }
        return [idx[0],playlist];
    }

    shuffle(index,change_idx) {
        let v = this.walk(index);
        let idx = parseInt(v[0]);
        let first_idx = idx + change_idx;
        let temp = v[1][first_idx];
        v[1][first_idx] = v[1][idx];
        v[1][idx] = temp;
    }

    handleUp(index) {
        this.shuffle(index,-1);
        this.setState({schedule: this.state.schedule,
                       history: this.state.history
                       .concat([this.state.schedule])});
    }

    handleDown(index) {
        this.shuffle(index,1);
        this.setState({schedule: this.state.schedule,
                       history: this.state.history
                       .concat([this.state.schedule])});
    }

    handleAdd(index,object) {
        find = index.length > 0 ? this.walk(index) :
            [undefined,this.state.schedule.playlists];
        index = find[0];
        object.id = this.counter.nextId();
        if(index == undefined) {
            find[1].push(object);
        } else {
            find[1][index]['playlists'].push(object);
        }
        this.setState({
            schedule: this.state.schedule,
            history: this.state.history.concat(this.state.schedule)
        });
    }

    handleDel(index) {
        find = this.walk(index);
        index = find[0];
        find[1].splice(index,1);
        this.setState({
            schedule: this.state.schedule,
            history: this.state.history.concat(this.state.schedule)
        });
    }

    multiChange(index,step,start) {
        console.log("index " + index.toString() + " step: " + step +
                    " start: " + start);
        find = this.walk(index);
        index = find[0];
        let tochange = find[1][index];
        tochange['step'] = step;
        tochange['start'] = start;
        this.setState({schedule: this.state.schedule,
                       history: this.state.history.concat(this.state.schedule)});
    }

    addId(index,id) {
        const find = this.walk(index);
        index = find[0];
        find[1][index].id = id;
        this.setState({
            schedule: this.state.schedule 
        });
    }

}

function ClickTab(props) {
    return(
        <button
          className={ props.selected ? "ClickTabSelected" : "ClickTab" }
          onClick={() => props.handleButton(props.keyName)}>
          {props.keyName.toUpperCase()}
        </button>
    );
}

function length(props) {
    return {
        'playlist': () => props.length,
        'merge': () => props.playlists.map(length).reduce((a,b) => a + b,0),
        'multi': () => length(props.playlist)/props.step,
        'complex': () => props.playlists.map(length)
            .reduce((a,b) =>
                    Math.max(a,b),0) * props.playlists.length,
    }[props.type]();
}

function makeUpDown(props,leftright,collapseExpand,handler) {
    let result = [];
    let currentIndex = props.index[props.index.length-1];
    let up = "up";
    let down = "down";
    if (leftright == true) {
        up = "left";
        down = "right";
    }
    if(currentIndex != 0) {
        result.push(
            <Up up={props.handlers.handleUp}
                key="up"
                symbol={up}
                index={props.index}
            />);
    } else {
        result.push(<button key="blank1" className="blank-up"></button>);
    }
    result.push(<Del del={props.handlers.handleDel}
                     key="del"
                     symbol="del"
                     index={props.index}
                />);
    if(collapseExpand != undefined) {
        result.push(<CollapseExpand collExp={collapseExpand}
                                    key="collexp"
                                    handler={handler} />);
    } else {
        result.push(<button key="blank2" className="blank-up"></button>);
    }
    if(currentIndex != props.lastIndex) {
        result.push(
            <Down down={props.handlers.handleDown}
                  key="down"
                  symbol={down}
                  index={props.index}
            />);
    }
    return (<div className="upDown">{result}</div>);
}

function Length(props) {
    return (<span>Length: {length(props)}</span>);
}

function Up(props) {
    return (<img className="up"
                 onClick={() => props.up(props.index)}
                 src={'image/' + props.symbol + '-arrow.png'} />);
}

function Del(props) {
    return (<img className="del"
              onClick={() => props.del(props.index)}
              src={'image/' + props.symbol + '.png'}
          />);
}

function Down(props) {
    return (<img className="down"
           onClick={() => props.down(props.index)}
           src={'image/' + props.symbol + '-arrow.png'}
     />);
}

function CollapseExpand(props) {
    return (<img className="collExp"
                 onClick={props.handler}
                 src={'image/' + props.collExp + '.png'}
    />);
}

function dispatch(handlers,index,lastIndex,obj,id) {
    return {
        'playlist': (<Playlist index={index}
                               key={obj.id+index}
                               lastIndex={lastIndex}
                               handlers={handlers}
                               type={obj.type}
                               name={obj.name}
                               length={obj.length} />),
        'merge': (<Merge type={obj.type}
                         index={index}
                         key={obj.id+index}
                         lastIndex={lastIndex}
                         handlers={handlers}
                         playlists={obj.playlists} />),
        'multi': (<Multi playlist={obj.playlist}
                         index={index}
                         key={obj.id+index}
                         handlers={handlers}
                         lastIndex={lastIndex}
                         type={obj.type}
                         start={obj.start}
                         step={obj.step} />),
        'complex': (<Complex type={obj.type}
                             index={index}
                             key={obj.id+index}
                             lastIndex={lastIndex}
                             handlers={handlers}
                             playlists={obj.playlists}
                    />)
    }[obj.type];
}

function schedule_dispatch(handlers,index,lastIndex,obj,id) {
    return {
        'playlist': (<PlaylistHead type={obj.type}
                                   index={index}
                                   key={obj.id+index}
                                   lastIndex={lastIndex}
                                   handlers={handlers}
                                   name={obj.name}
                                   length={obj.length} />),
        'merge': (<Merge type={obj.type}
                         index={index}
                         key={obj.id+index}
                         lastIndex={lastIndex}
                         handlers={handlers}
                         playlists={obj.playlists}/>),
        'multi': (<Multi playlist={obj.playlist}
                         index={index}
                         key={obj.id+index}
                         lastIndex={lastIndex}
                         handlers={handlers}
                         type={obj.type}
                         start={obj.start}
                         step={obj.step} />),
        'complex': (<Complex type={obj.type}
                             index={index}
                             key={obj.id+index}
                             lastIndex={lastIndex}
                             handlers={handlers}
                             playlists={obj.playlists}
                    />)
    }[obj.type];
}

function HiddenElement(props) {
    return (<div className="hidden"
                 style={{display: (props.hide ? "none" : "block")}}>
              {props.children}
              </div>);
}

function Playlist(props) {
    const up_down = makeUpDown(props);
    return (
        <li>
          {up_down}
          {props.name}: {props.length}
        </li>);
}

function PlaylistHead(props) {
    const up_down = makeUpDown(props,true);
    return (
        <div className="playlist block">
          {up_down}
          <div className="header">
            <strong>Playlist {props.name}
              <br/>
              <Length type={props.type}
                      length={props.length} />
            </strong>
          </div>
        </div>
    );
}

class Merge extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            props: props,
            hidden: true
        };
        this.handleClick = this.handleClick.bind(this);
        this.counter = new Counter();
    }

    handleClick() {
        this.setState({hidden:!this.state.hidden});
    }

    render() {
        const props = this.state.props;
        const result = make_indexed_elements(props,
                                             props.index,
                                             props.playlists.length-1,
                                             this.counter);
        const up_down = makeUpDown(props,true,
                                   this.state.hidden ?
                                   'expand' :
                                   'collapse',this.handleClick);

        return (
            <div className="merge block">
              {up_down}
              <div className="header">
                <strong>Merge
                  <br/>
                  <Length type={props.type}
                          playlists={props.playlists}/></strong>
              </div>
              <HiddenElement hide={this.state.hidden}>
                <MakeElement handlers={props.handlers}
                             index={props.index} />
                <ol>{result}</ol>
              </HiddenElement>
            </div>
        );
    }
}

class Multi extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            props: props,
            hidden: true,
            start: props.start,
            step: props.step
        };
        this.handleClick = this.handleClick.bind(this);
        // this.handleStep = this.handleStep.bind(this);
        // this.handleStart = this.handleStart.bind(this);
        this.counter = new Counter();
    }

    handleClick() {
        this.setState({hidden:!this.state.hidden});
    }

    handleStep(evt) {
        this.setState({step:parseInt(evt.target.value)});
    }

    handleStart(evt) {
        this.setState({start:parseInt(evt.target.value)});
    }

    render() {
        const props = this.state.props;
        const result = make_indexed_elements(props,props.index,0,this.counter);
        const up_down = makeUpDown(props,true,
                                   this.state.hidden ?
                                   'expand' :
                                   'collapse',
                                   this.handleClick);

        return (
            <div className="multi block">
              {up_down}
              <div className="header" >
                <strong>Multi
                  <br/>
                  <Length type={props.type}
                          step={props.step}
                          playlist={props.playlist}/>
                  <br/>
                  <label htmlFor="step">Step: </label>
                  <input name="step" type="text"
                         value={this.state.step}
                         onBlur={(evt) => props.handlers
                                 .multiChange(props.index,
                                              parseInt(evt.target.value),
                                              this.state.start)}
                         onChange={(evt) => this.handleStep(evt)}
                  />
                  <br/>
                  <label htmlFor="start">Start: </label>
                  <input name="start" type="text"
                         value={this.state.start}
                         onBlur = {(evt) =>
                                   props.handlers
                                   .multiChange(props.index,
                                                this.state.step,
                                                parseInt(evt.target.value))}
                         onChange={(evt) => this.handleStart(evt)}
                  />
                </strong>

              </div>
              <HiddenElement hide={this.state.hidden}>
                {result}
              </HiddenElement>
            </div>
        );
    }
}

class Complex extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            props: props,
            hidden: true
        };
        this.handleClick = this.handleClick.bind(this);
        this.counter = new Counter();
    }

    handleClick() {
        this.setState({hidden:!this.state.hidden});
    }

    render() {
        const props = this.state.props;
        const result = make_indexed_elements(props, props.index,
                                             props.playlists.length-1,this.counter);
        const index = props.index;
        const up_down = makeUpDown(props,true,
                                   this.state.hidden ?
                                   'expand' :
                                   'collapse',
                                   this.handleClick);

        return (
            <div className="complex block">
              {up_down}
              <div className="header" >
                <strong>Complex
                  <br/>
                  <Length type={props.type}
                          playlists={props.playlists}/></strong>
              </div>
              <HiddenElement hide={this.state.hidden}>
                <MakeElement handlers={props.handlers}
                             index={props.index} />
                <ul>{result}</ul>
              </HiddenElement>
            </div>
        );
    }
}

function make_indexed_elements(props,index,lastIndex,counter) {
    const result = [];
    if(undefined != props.playlist) {
        let t = index.slice();
        t.push(0);
        result.push(dispatch(props.handlers,t,0,props.playlist));
        return result;
    }
    for (let i in props.playlists) {
        let t = index.slice();
        t.push(i);
        result.push(dispatch(props.handlers,
                             t, lastIndex,
                             props.playlists[i]));
    }
    return result;
}

class Schedule extends React.Component {
    constructor(props) {
        super(props);
        this.counter = new Counter();
        this.state = {props: props};
    }

    render() {
        const props = this.state.props;
    const result = [];
    const lastIndex = props.playlists.length-1;
    for (let i in props.playlists) {
        result.push(schedule_dispatch(props.handlers
                                      ,[i]
                                      ,lastIndex
                                      ,props.playlists[i]
                                      ,this.counter));
    }

    return (
        <div id="schedule">
          <MakeElement handlers={props.handlers}
                       index={[]} />
          <ol className="schedule">
            {result}
          </ol>
        </div>
    );
}
}
class MakeElement extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            selectValue: 'playlist',
            selectPlaylist: all_playlists[0].props.value
        };
        this.handleChange = this.handleChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
        this.handlePlaylistChange = this.handlePlaylistChange.bind(this);
        this.dispatch = this.dispatch.bind(this);
        this.handleAdd = props.handlers.handleAdd;
        this.index = props.index;
    }

    handleChange(event) {
        this.setState({selectValue: event.target.value});
    }

    handlePlaylistChange(event) {
        this.setState({selectPlaylist: event.target.value});
    }

    dispatch() {
        let pattern = /(.*) (\d+)/;
        let result = pattern.exec(this.state.selectPlaylist);

        return ({
            'playlist': {'type':'playlist',
                         'name':result[1],
                         'length':parseInt(result[2])},
            'merge': {'type':'merge',
                      'playlists':[]},
            'multi': {'type':'multi',
                      'playlist':{"type":"merge","playlists":[]},
                      'step':0,
                      'start':0},
            'complex': {'type':'complex',
                        'playlists':[]}
        }[this.state.selectValue]);
    }

    handleSubmit(event) {
        this.handleAdd(this.index,this.dispatch());
        event.preventDefault();
    }

    render() {
        return (
            <div className="make">
              <select onChange={this.handlePlaylistChange}>{all_playlists}</select>
              <select
                value={this.state.selectValue}
                onChange={this.handleChange}>
                <option key="complex" value="complex">complex</option>
                <option key="merge" value="merge">merge</option>
                <option key="multi" value="multi">multi</option>
                <option key="playlist" value="playlist">playlist</option>
              </select>
              <input name="add"
                     value="Add"
                     type="submit"
                     onClick={this.handleSubmit}
              />
            </div>
        );
    }
}

var all_playlists = [];

document.addEventListener("DOMContentLoaded", () => {
    let t = document.querySelector('#playlist').querySelectorAll('option');
    t.forEach((item) =>
              all_playlists
              .push(<option key={item.text} value={item.text}>
    {item.text}
  </option>));
    const mode = document.querySelector("input[name='mode']").value;
    const domContainer = document.querySelector('#react');
    const schedule = document.querySelector("textarea[name='schedule-body']");
    const schedJson = schedule ? JSON.parse(schedule.textContent)
          : {name:'',playlists:[]};
    ReactDOM.render(e(Content,{schedule:schedJson,mode:mode}), domContainer);
});
