import { ChangeDetectionStrategy, Component, HostListener, Inject } from '@angular/core';
import { I18NService } from '@core';
import { ALAIN_I18N_TOKEN } from '@delon/theme';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalService } from 'ng-zorro-antd/modal';

@Component({
  selector: 'header-switchnet',
  template: `
    <i nz-icon class="mr-sm" nzType="tool"></i>
    切换网络
  `,
  host: {
    '[class.d-block]': 'true'
  },
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SwitchnetComponent {
  constructor(
    private modalSrv: NzModalService,
    private messageSrv: NzMessageService,
    @Inject(ALAIN_I18N_TOKEN) private i18nSvc: I18NService
  ) {}

  @HostListener('click')
  _click(): void {
    this.modalSrv.confirm({
      nzTitle: localStorage.getItem('net')==='prod'?'要切换到办公网吗？':'要切换到生产网吗？',
      nzOnOk: () => {
        var item = localStorage.getItem('net');
        console.log("====>"+item);
        if(item==='prod'){
          localStorage.setItem('net','office')
        }else{
          localStorage.setItem('net','prod')
        }
        this.messageSrv.success('切换成功');
      }
    });
  }
}
